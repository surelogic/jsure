package com.surelogic.javac.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;

/**
 * Contains information about what scans exist in a JSure data directory.
 */
public class JSureDataDir {

	private final File f_dir;

	private final List<JSureScan> f_scans;

	private final Map<String, JSureScan> f_projectToScan = new HashMap<String, JSureScan>();

	JSureDataDir(File dir, List<JSureScan> scans,
			Map<String, JSureScan> projectToScan) throws IOException {
		f_dir = dir;
		f_scans = scans;
		f_projectToScan.putAll(projectToScan);

		for (Map.Entry<String, JSureScan> e : projectToScan.entrySet()) {
			e.getValue().getLatestFilesForProject(e.getKey());
		}
	}

	public File getDir() {
		return f_dir;
	}

	public synchronized JSureScan findScan(File location) {
		JSureScan rv = null;
		for (JSureScan r : f_scans) {
			if (r.getDir().equals(location)) {
				return r;
			}
		}
		return rv;
	}

	public synchronized boolean contains(JSureScan scan) {
		return f_scans.contains(scan);
	}

	public synchronized List<JSureScan> getScans() {
		return new ArrayList<JSureScan>(f_scans);
	}

	public synchronized JSureScan[] getScansAsArray() {
		return f_scans.toArray(new JSureScan[f_scans.size()]);
	}

	synchronized List<JSureScan> getScansOnDiskRightNow() {
		/*
		 * Collect together existing information about scans
		 */
		final Map<File, JSureScan> knownScans = new HashMap<File, JSureScan>();
		for (JSureScan r : f_scans) {
			do {
				knownScans.put(r.getDir(), r);
				r = r.getLastPartialScan();
			} while (r != null);
		}
		/*
		 * Look for scan directories
		 */
		final List<JSureScan> scans = new ArrayList<JSureScan>();
		for (File f : f_dir.listFiles()) {
			JSureScan run = knownScans.get(f);
			if (run == null) {
				// This is a new directory
				run = JSureDataDirScanner.findRunDirectory(f);
			}
			if (run != null) {
				scans.add(run);
			}
		}
		return scans;
	}

	/**
	 * Find the latest scan that scans the same projects as the one specified
	 */
	public synchronized JSureScan findLastMatchingScan(JSureScan scan) {
		if (scan == null) {
			return null;
		}
		try {
			scan.getProjects();
		} catch(Exception e) {
			SLLogger.getLogger().log(Level.WARNING, "Unable to determine projects for "+scan.getDirName(), e);
			return null;
		}
		JSureScan match = null;
		for(JSureScan s : f_scans) {
			if (s == scan || scan.getDir().equals(s.getDir())) {
				continue;
			}
			try {
				if (scan.getProjects().matchProjects(s.getProjects()) && s.getTimeOfScan().before(scan.getTimeOfScan())) {
					if (match == null || s.getTimeOfScan().after(match.getTimeOfScan())) {
						match = s;
					}
				}
			} catch (Exception e) {
				SLLogger.getLogger().log(Level.WARNING, "Unable to determine projects for "+scan.getDirName(), e);
				continue;
			}
		}
		return match;
	}
}
