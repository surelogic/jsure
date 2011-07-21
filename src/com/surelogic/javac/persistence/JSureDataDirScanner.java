package com.surelogic.javac.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.surelogic.javac.JavacProject;
import com.surelogic.javac.Projects;

/**
 * Scans and organizes the run directories in the JSure data directory.
 */
public class JSureDataDirScanner {

	public static JSureScan findRunDirectory(File f) {
		try {
			return new JSureScan(f);
		} catch (Exception e) {
			// Bad date
			return null;
		}
	}

	public static JSureDataDir scan(JSureDataDir oldData) {
		return organizeRuns(oldData.getDir(), oldData.updateRuns());
	}

	public static JSureDataDir scan(File dataDir) {
		final Map<String, JSureScan> runs = new HashMap<String, JSureScan>();

		// Look for run directories
		for (File f : dataDir.listFiles()) {
			final JSureScan run = findRunDirectory(f);
			if (run != null) {
				runs.put(run.getName(), run);
			}
		}
		return organizeRuns(dataDir, runs);
	}

	private static JSureDataDir organizeRuns(File dataDir,
			Map<String, JSureScan> runs) {
		/*
		 * Figure out which are the full runs, and which are the last partial
		 * runs.
		 */
		final List<JSureScan> full = new ArrayList<JSureScan>();
		// These should end up to be the last in a series
		final Set<JSureScan> roots = new HashSet<JSureScan>(runs.values());
		for (JSureScan run : runs.values()) {
			try {
				final Projects p = run.getProjects();
				final String lastName = p.getLastRun();
				if (lastName != null) {
					// This one is a partial run and depends on the last one
					final JSureScan last = runs.get(lastName);
					if (last == null) {
						System.err.println("Couldn't find run: " + last
								+ " -> " + run.getName());
						roots.remove(run);
					} else {
						// The last run is not a root
						roots.remove(last);
					}
					run.setLastRun(last);
				} else {
					full.add(run);
				}
			} catch (Exception e) {
				// This should never happen
				e.printStackTrace();
			}
		}
		List<JSureScan> partials = new ArrayList<JSureScan>(roots);
		// Sorted: oldest first
		Collections.sort(partials);
		Collections.sort(full);

		Map<JSureScan, JSureScan> fullToPartial = new HashMap<JSureScan, JSureScan>();
		for (final JSureScan root : partials) {
			// Find the corresponding full run
			JSureScan run = root;
			while (run.getLastRun() != null) {
				run = run.getLastRun();
			}
			fullToPartial.put(run, root);
		}
		checkFullRuns(full, fullToPartial);

		// Collect which projects map to which runs?
		final Map<String, JSureScan> project2run = new HashMap<String, JSureScan>();
		for (Map.Entry<JSureScan, JSureScan> e : fullToPartial.entrySet()) {
			try {
				final Projects projs = e.getKey().getProjects();
				for (JavacProject p : projs) {
					project2run.put(p.getName(), e.getValue());
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		try {
			return new JSureDataDir(dataDir, runs, project2run);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Check if all of full has a mapping, and has results.
	 */
	private static void checkFullRuns(List<JSureScan> full,
			Map<JSureScan, JSureScan> fullToPartial) {
		for (JSureScan run : full) {
			if (!fullToPartial.containsKey(run)) {
				System.out.println("No partials for " + run);
			}
			try {
				// Check for results
				final File results = new File(run.getDir(),
						PersistenceConstants.RESULTS_ZIP);
				if (!results.exists()) {
					// System.out.println("No results for full run "+run);
					continue;
				}
				// Collect up all the sources
				final Set<String> sources = new HashSet<String>();
				for (File src : new File(run.getDir(), "zips").listFiles()) {
					if (src.isFile() && src.getName().endsWith(".zip")) {
						ZipFile zf = new ZipFile(src);
						Enumeration<? extends ZipEntry> e = zf.entries();
						while (e.hasMoreElements()) {
							ZipEntry ze = e.nextElement();
							String path = ze.getName();
							if (path.endsWith(".java")) {
								sources.add(path);
							}
						}
						zf.close();
					}
				}
				// Check if we have results for each of them
				ZipFile resultsZip = new ZipFile(results);
				for (String path : sources) {
					if (resultsZip.getEntry(path) == null) {
						System.out.println("No results for " + path);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}