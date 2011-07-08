package com.surelogic.javac.persistence;

import java.io.*;
import java.util.*;

public class JSureDataDir {
	private final File dataDir;
	private final Map<String, JSureRun> runs;
	private final Map<String, JSureRun> project2run = new HashMap<String, JSureRun>();

	JSureDataDir(File dir, Map<String, JSureRun> runs, Map<String, JSureRun> p2r)
			throws IOException {
		dataDir = dir;
		this.runs = runs;
		project2run.putAll(p2r);

		for (Map.Entry<String, JSureRun> e : p2r.entrySet()) {
			e.getValue().getLatestFilesForProject(e.getKey());
		}
	}

	// TODO will I really do this by project?
	// Assumes that we have results data for all these ... probably should check
	public File getDataDir() {
		return dataDir;
	}

	public synchronized JSureRun findScan(File location) {
		JSureRun rv = runs.get(location.getName());
		if (rv == null) {
			// Look at each one
			for (JSureRun r : runs.values()) {
				if (r.getDir().equals(location)) {
					return r;
				}
			}
		}
		return rv;
	}

	public synchronized JSureRun[] getAllRuns() {
		return runs.values().toArray(new JSureRun[runs.size()]);
	}

	// Modified from JSureDataDirScanner.scan(File)
	Map<String, JSureRun> updateRuns() {
		// Collect together existing info about runs
		final Map<File, JSureRun> oldInfo = new HashMap<File, JSureRun>();
		for (JSureRun r : runs.values()) {
			do {
				oldInfo.put(r.getDir(), r);
				r = r.getLastRun();
			} while (r != null);
		}
		// Look for run directories
		final Map<String, JSureRun> runs = new HashMap<String, JSureRun>();
		for (File f : dataDir.listFiles()) {
			JSureRun run = oldInfo.get(f);
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
