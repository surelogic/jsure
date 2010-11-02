package edu.cmu.cs.fluid.dcf.views.coe;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.surelogic.fluid.eclipse.preferences.PreferenceConstants;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.ProjectsDrop;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot.Info;

public class PersistentResultsView extends ResultsView {
  private static final String NAME = "snapshot"+SeaSnapshot.SUFFIX;
  
  /**
   * TODO Mainly used to store ProposedPromiseDrops?
   * (can this really operate w/o the IRNodes?)
   */
  final Sea sea = Sea.getDefault();//new Sea();
  
  final File location;
  Collection<Info> dropInfo = Collections.emptyList();
	
  public PersistentResultsView() {
	  File location = null;
	  try {
		  final File jsureData = PreferenceConstants.getJSureDataDirectory();
		  if (jsureData != null) {
			  location = new File(jsureData, NAME);
		  } else {
			  location = File.createTempFile("snapshot", SeaSnapshot.SUFFIX);
		  }   
	  } catch(IOException e) {
		  // Nothing to do
	  }
	  this.location = location;
  }
  
  @Override
  public void analysisStarting() {
	  if (location == null || !location.exists()) {
		  super.analysisStarting();
	  }
	  // Ignore this, so we can continue to look at the old results
  }
  
  @Override 
  public void seaChanged() {
	  if (location == null || !location.exists()) {
		  super.seaChanged();
	  } else { 
		  // load it up
		  finishCreatePartControl();
	  }
  }
  
  @Override
  protected void finishCreatePartControl() {
	  if (location != null && location.exists()) {
		  try {
			  dropInfo = SeaSnapshot.loadSnapshot(location);
			  // TODO restore viewer state?
			  provider.buildModelOfDropSea_internal();
			  setViewerVisibility(true);
			  System.out.println("Loaded snapshot");
		  } catch (Exception e) {
			  e.printStackTrace();
			  dropInfo = Collections.emptyList();
		  }
	  }
  }
  
  GenericResultsViewContentProvider<Info,Content> provider;
  
  @Override
  protected IResultsViewContentProvider makeContentProvider() {
	  return new GenericResultsViewContentProvider<Info,Content>(sea) {		
		{
			provider = this;
		}
		@Override
    	public IResultsViewContentProvider buildModelOfDropSea() {
    		try {      		
        		// Persist the Sea, and then load the info    
    			new SeaSnapshot(location).snapshot(ProjectsDrop.getDrop().getIIRProjects().getLabel(), Sea.getDefault());
    			// TODO save viewer state?
    			saveViewState();
    			if (location != null && location.exists() && location.length() > 0) {
    				dropInfo = SeaSnapshot.loadSnapshot(location);
    			}
    		} catch (Exception e) {
    			dropInfo = Collections.emptyList();
    		}
    		try {
    			return super.buildModelOfDropSea_internal();
    		} finally {
    			f_viewerbook.getDisplay().asyncExec(new Runnable() {
    				public void run() {
    					restoreViewState();
    				}
    			});
    		}
    	}
    	
		@Override
		protected boolean dropsExist(Class<? extends Drop> type) {
			for(Info i : dropInfo) {
				if (i.isInstance(type)) {
					return true;
				}
			}
			return false;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected <R extends IDropInfo> 
		Collection<R> getDropsOfType(Class<? extends Drop> type, Class<R> rType) {
			List<R> rv = new ArrayList<R>();
			for(Info i : dropInfo) {
				if (i.isInstance(type)) {
					rv.add((R) i);
				}
			}
			return rv;
		}
		
		@Override
		protected Content makeContent(String msg) {
			return new Content(msg, Collections.<Content>emptyList(), null);
		}
		
		@Override
		protected Content makeContent(String msg, Collection<Content> contentRoot) {
			return new Content(msg, contentRoot, null);
		}

		@Override
		protected Content makeContent(String msg, Info drop) {
			return new Content(msg, Collections.<Content>emptyList(), drop);
		}

		@Override
		protected Content makeContent(String msg, ISrcRef ref) {
			return new Content(msg, ref);
		}
    };
  }
  
  static class Content extends AbstractContent<Info,Content>{
	Content(String msg, Collection<Content> content, Info drop) {
		super(msg, content, drop);
	}	  
	Content(String msg, ISrcRef ref) {
		super(msg, ref);
	}
  }
}
