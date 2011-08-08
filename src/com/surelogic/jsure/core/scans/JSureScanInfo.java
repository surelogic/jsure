package com.surelogic.jsure.core.scans;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.javac.Projects;
import com.surelogic.javac.jobs.RemoteJSureRun;
import com.surelogic.javac.persistence.JSureScan;

import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IDropInfo;
import edu.cmu.cs.fluid.sea.IProofDropInfo;
import edu.cmu.cs.fluid.sea.drops.ProjectsDrop;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;

/**
 * Manages the project information, the loading of drop information and other
 * statistics about a particular JSure scan on the disk.
 */
public class JSureScanInfo {
	/**
	 * For testing of this class. Used to test how long the loading takes.
	 */
	private static final boolean skipLoading = false;

	private List<IDropInfo> f_dropInfo = null;

	private final JSureScan f_run; // non-null

	public JSureScanInfo(JSureScan run) {
		if (run == null)
			throw new IllegalArgumentException(I18N.err(44, "run"));
		f_run = run;
	}

	public synchronized JSureScan getJSureRun() {
		return f_run;
	}

	public synchronized Projects getProjects() {
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

	private synchronized List<IDropInfo> loadOrGetDropInfo() {
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

	public synchronized File getDir() {
		return f_run.getDir();
	}

	public synchronized String getLabel() {
		return f_run.getDir().getName();
	}

	public synchronized boolean isEmpty() {
		return loadOrGetDropInfo().isEmpty();
	}

	public synchronized List<IDropInfo> getDropInfo() {
		return loadOrGetDropInfo();
	}

	public synchronized boolean dropsExist(Class<? extends Drop> type) {
		for (IDropInfo i : loadOrGetDropInfo()) {
			if (i.isInstance(type)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public synchronized <T extends IDropInfo, T2 extends Drop> Set<T> getDropsOfType(
			Class<T2> dropType) {
		List<IDropInfo> info = loadOrGetDropInfo();
		if (!info.isEmpty()) {
			final Set<T> result = new HashSet<T>();
			for (IDropInfo i : info) {
				if (i.isInstance(dropType)) {
					result.add((T) i);
				}
			}
			return result;
		}
		return Collections.emptySet();
	}

	public synchronized List<IProofDropInfo> getProofDropInfo() {
		final List<IProofDropInfo> result = new ArrayList<IProofDropInfo>();
		for (IDropInfo i : loadOrGetDropInfo()) {
			if (i instanceof IProofDropInfo) {
				final IProofDropInfo ipd = (IProofDropInfo) i;
				result.add(ipd);
			}
		}
		return result;
	}

	public synchronized String findProjectsLabel() {
		for (IDropInfo info : getDropsOfType(ProjectsDrop.class)) {
			return info.getAttribute(AbstractXMLReader.PROJECTS);
		}
		return null;
	}
}
