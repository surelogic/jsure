package edu.cmu.cs.fluid.dcf.views.coe;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.surelogic.common.FileUtility;
import com.surelogic.common.ui.ViewUtility;
import com.surelogic.fluid.eclipse.preferences.PreferenceConstants;
import com.surelogic.fluid.javac.Projects;
import com.surelogic.fluid.javac.jobs.RemoteJSureRun;

import edu.cmu.cs.fluid.dc.*;
import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.ProjectsDrop;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot.Info;

public class PersistentDropInfo implements IAnalysisListener, SeaObserver {
	private static final String NAME = "snapshot"+SeaSnapshot.SUFFIX;
	public static final boolean useInfo = true;
	
	private long timestamp = Long.MIN_VALUE;	
	private Collection<Info> dropInfo = Collections.emptyList();
	private final List<AbstractDoubleCheckerView> listeners = new CopyOnWriteArrayList<AbstractDoubleCheckerView>();
	private final File location;
	
	private PersistentDropInfo() {
		File location = null;
		try {
			final File jsureData = PreferenceConstants.getJSureDataDirectory();
			if (jsureData != null) {
				location = new File(jsureData, NAME);
			} else {
				location = File.createTempFile("snapshot", SeaSnapshot.SUFFIX);
			}   
			//System.out.println("Using location: "+location);
		} catch(IOException e) {
			// Nothing to do
		}
		this.location = location;
		System.out.println("Drop location = "+location);
		
		// subscribe to listen for analysis notifications
		NotificationHub.addAnalysisListener(this);
		Sea.getDefault().addSeaObserver(this);
	}
	
	private static final PersistentDropInfo instance = new PersistentDropInfo();
	
	public static PersistentDropInfo getInstance() {
		return instance;
	}

	public static void showDrop(IProofDropInfo d) {
		if (useInfo) {
			final PersistentResultsView view = (PersistentResultsView) ViewUtility.showView(PersistentResultsView.class.getName());
			view.showDrop(d);
		} else {
			final ResultsView view = (ResultsView) ViewUtility.showView(ResultsView.class.getName());
			if (view != null && d instanceof ProofDrop) {
				view.showDrop((ProofDrop) d);
			}
		}
	}
	
	public boolean load() {
		try {
			final ProjectsDrop drop = ProjectsDrop.getDrop();			
			// TODO NPE since it's built externally
			if (drop != null) {
				final Projects projects = (Projects) drop.getIIRProjects();
				final File results      = new File(projects.getRunDir(), RemoteJSureRun.RESULTS_XML);
				if (results.exists() && results.length() > 0) {
					if (location != null) {
						FileUtility.copy(results, location);
						System.out.println("Copying results from "+results);
					}
				} else {
					// Persist the Sea, and then load the info    
					new SeaSnapshot(location).snapshot(projects.getLabel(), Sea.getDefault());
				}
			}
			if (location != null && location.exists()) {
				final long lastModified = location.lastModified();
				if (dropInfo.isEmpty() || lastModified > timestamp) {
					System.out.println("Loading info at "+lastModified);
					dropInfo = SeaSnapshot.loadSnapshot(location);
					timestamp = lastModified;
				}
				return true;
			} else {
				dropInfo = Collections.emptyList();
			}
		} catch (Exception e) {
			dropInfo = Collections.emptyList();
		}
		return false;
	}
	
	public boolean isEmpty() {
		return dropInfo.isEmpty();
	}
	
	public synchronized boolean dropsExist(Class<? extends Drop> type) {
		for(Info i : dropInfo) {
			if (i.isInstance(type)) {
				return true;
			}
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized <T extends IDropInfo, T2 extends Drop> Set<T> getDropsOfType(Class<T2> dropType) {
		if (useInfo) {
			if (!dropInfo.isEmpty()) {
				final Set<T> result = new HashSet<T>();			
				for(Info i : dropInfo) {
					if (i.isInstance(dropType)) {
						result.add((T) i);
					}
				}
				return result;
			}
			return Collections.emptySet();
		}
		return (Set<T>) Sea.getDefault().getDropsOfType(dropType);
	}

	public void addListener(AbstractDoubleCheckerView v) {
		listeners.add(v);
	}

	@Override
	public void analysisStarting() {
		if (!useInfo || location == null || !location.exists()) {
			for(AbstractDoubleCheckerView v : listeners) {
				v.analysisStarting();
			}
		}
	}
	
	@Override
	public void analysisPostponed() {
		for(AbstractDoubleCheckerView v : listeners) {
			v.analysisPostponed();
		}
	}
	
	@Override
	public void analysisCompleted() {
		if (!useInfo || load()) {		
			for(AbstractDoubleCheckerView v : listeners) {
				v.analysisCompleted();
			}
		}
	}

	@Override
	public void seaChanged() {
		if (!useInfo) {
			for(AbstractDoubleCheckerView v : listeners) {
				v.seaChanged();
			}
		}
	}
}
