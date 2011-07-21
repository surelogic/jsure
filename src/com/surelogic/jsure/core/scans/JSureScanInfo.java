package com.surelogic.jsure.core.scans;

import java.io.File;
import java.util.*;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.javac.Projects;
import com.surelogic.javac.jobs.RemoteJSureRun;
import com.surelogic.javac.persistence.*;

import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.ProjectsDrop;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot.Info;

/**
 * Manages the project information, the loading of drop information and other
 * statistics about a particular JSure scan on the disk.
 */
public class JSureScanInfo {
	/**
	 * For testing of this class. Used to test how long the loading takes.
	 */
	private static final boolean skipLoading = false;

	private Collection<Info> f_dropInfo = null;

	private final JSureScan f_run; // non-null

	public JSureScanInfo(JSureScan run) {
		if (run == null)
			throw new IllegalArgumentException(I18N.err(44, "run"));
		f_run = run;
	}

	public JSureScan getJSureRun() {
		return f_run;
	}

	public Projects getProjects() {
		try {
			if (f_run == null) {
				return null;
			}
			return f_run.getProjects();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private Collection<Info> loadInfo() {
		if (f_dropInfo != null) {
			return f_dropInfo;
		}
		final long start = System.currentTimeMillis();
		System.out.println("Loading info at " + start);
		try {
			if (skipLoading) {
				throw new Exception("Skipping loading");
			}
			f_dropInfo = SeaSnapshot.loadSnapshot(new File(f_run.getDir(),
					RemoteJSureRun.RESULTS_XML));
			final long end = System.currentTimeMillis();
			System.out.println("Finished loading info = " + (end - start)
					+ " ms");
		} catch (Exception e) {
			e.printStackTrace(); // TODO
			f_dropInfo = Collections.emptyList();
		}
		return f_dropInfo;
	}

	public File getDir() {
		return f_run.getDir();
	}

	public synchronized String getLabel() {
		return f_run.getDir().getName();
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
		for (IDropInfo info : getDropsOfType(ProjectsDrop.class)) {
			return info.getAttribute(AbstractXMLReader.PROJECTS);
		}
		return null;
	}
}
