package com.surelogic.fluid.javac.scans;

import java.io.File;
import java.util.*;

import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.fluid.javac.Projects;
import com.surelogic.fluid.javac.jobs.RemoteJSureRun;
import com.surelogic.fluid.javac.persistence.*;

import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.ProjectsDrop;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot.Info;

/**
 * Manages the project info, the loading of drop info and other 
 * statistics
 * 
 * @author Edwin
 */
public class JSureScanInfo {
	private static final boolean skipLoading = false;
	
	private final File location;
	private Collection<Info> dropInfo = null;
	private final JSureRun scan;
	
	public JSureScanInfo(File dir) {
		location = dir;		
		JSureRun run = null;
		try {
			run = new JSureRun(dir);
		} catch (Exception e) {
			e.printStackTrace();
		}
		scan = run;
	}
	
	public Projects getProjects() {
		try {
			if (scan == null) {
				return null;
			}
			return scan.getProjects();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private Collection<Info> loadInfo() {
		if (dropInfo != null) {
			return dropInfo;
		}
		final long start = System.currentTimeMillis();
		System.out.println("Loading info at " + start);
		try {
			if (skipLoading) {
				throw new Exception("Skipping loading");
			}
			dropInfo = SeaSnapshot.loadSnapshot(new File(location, RemoteJSureRun.RESULTS_XML));
			final long end = System.currentTimeMillis();
			System.out.println("Finished loading info = "+(end-start)+" ms");
		} catch (Exception e) {
			e.printStackTrace(); // TODO
			dropInfo = Collections.emptyList();
		}
		return dropInfo;
	}
	
	public File getLocation() {
		return location;
	}
	
	public synchronized String getLabel() {
		// TODO Auto-generated method stub
		return location.getName();
	}
	
	public synchronized boolean isEmpty() {
		return loadInfo().isEmpty();
	}

	public synchronized Collection<? extends IDropInfo> getRawInfo() {
		return loadInfo();
	}
	
	public synchronized boolean dropsExist(Class<? extends Drop> type) {
		for (Info i : loadInfo()) {
			if (i.isInstance(type)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public synchronized <T extends IDropInfo, T2 extends Drop> Set<T> getDropsOfType(
			Class<T2> dropType) {
		Collection<Info> info = loadInfo();
		if (!info.isEmpty()) {
			final Set<T> result = new HashSet<T>();
			for (Info i : info) {
				if (i.isInstance(dropType)) {
					result.add((T) i);
				}
			}
			return result;
		}
		return Collections.emptySet();
	}
	
	public synchronized String findProjectsLabel() {
		for(IDropInfo info : getDropsOfType(ProjectsDrop.class)) {
			return info.getAttribute(AbstractXMLReader.PROJECTS);
		}
		return null;
	}
}
