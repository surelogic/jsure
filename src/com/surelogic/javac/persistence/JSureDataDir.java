package com.surelogic.javac.persistence;

import java.io.*;
import java.util.*;

/**
 * Contains information about what scans or runs exist in a JSure data
 * directory.
 */
public class JSureDataDir {
	private final File f_dir;
	private final Map<String, JSureScan> f_scans;
	private final Map<String, JSureScan> f_projectToScan = new HashMap<String, JSureScan>();

	JSureDataDir(File dir, Map<String, JSureScan> runs, Map<String, JSureScan> p2r)
			throws IOException {
		f_dir = dir;
		this.f_scans = runs;
		f_projectToScan.putAll(p2r);

		for (Map.Entry<String, JSureScan> e : p2r.entrySet()) {
			e.getValue().getLatestFilesForProject(e.getKey());
		}
	}

	public File getDir() {
		return f_dir;
	}

	public synchronized JSureScan findScan(File location) {
		JSureScan rv = f_scans.get(location.getName());
		if (rv == null) {
			// Look at each one
			for (JSureScan r : f_scans.values()) {
				if (r.getDir().equals(location)) {
					return r;
				}
			}
		}
		return rv;
	}

	public synchronized JSureScan[] getAllRuns() {
		return f_scans.values().toArray(new JSureScan[f_scans.size()]);
	}

	synchronized Map<String, JSureScan> updateRuns() {
		// Collect together existing info about runs
		final Map<File, JSureScan> oldInfo = new HashMap<File, JSureScan>();
		for (JSureScan r : f_scans.values()) {
			do {
				oldInfo.put(r.getDir(), r);
				r = r.getLastRun();
			} while (r != null);
		}
		// Look for run directories
		final Map<String, JSureScan> runs = new HashMap<String, JSureScan>();
		for (File f : f_dir.listFiles()) {
			JSureScan run = oldInfo.get(f);
			if (run == null) {
				// This is a new directory
				run = JSureDataDirScanner.findRunDirectory(f);
			}
			if (run != null) {
				runs.put(run.getName(), run);
			}
		}
		return runs;
	}
}
