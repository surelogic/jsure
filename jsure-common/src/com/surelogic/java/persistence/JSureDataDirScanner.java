package com.surelogic.java.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.surelogic.common.StringCache;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.java.JavaProject;
import com.surelogic.common.java.JavaProjectSet;
import com.surelogic.common.java.PersistenceConstants;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.DeclUtil;

/**
 * Scans and organizes the scan directories in the JSure data directory.
 */
public class JSureDataDirScanner {

	public static JSureScan findRunDirectory(File f) {
		if (JSureScan.isValidScan(f)) {
			try {
				return new JSureScan(f);
			} catch (Exception e) {
				SLLogger.getLogger().log(Level.WARNING, I18N.err(228, f), e);
			}
		}
		return null;
	}

	public static JSureDataDir scan(JSureDataDir oldData) {
		return getDataDirWithRunsOrganized(oldData.getDir(),
				oldData.getScansOnDiskRightNow());
	}

	public static JSureDataDir scan(File dataDir) {
		final List<JSureScan> runs = new ArrayList<>();
		try {		
			DeclUtil.setStringCache(new StringCache());

			// Look for run directories
			for (File f : dataDir.listFiles()) {
				final JSureScan run = findRunDirectory(f);
				if (run != null) {
					runs.add(run);
				}
			}
			return getDataDirWithRunsOrganized(dataDir, runs);
		} finally {
			DeclUtil.setStringCache(null);
		}
	}

	private static JSureDataDir getDataDirWithRunsOrganized(File dataDir,
			List<JSureScan> scans) {
		/*
		 * Figure out which are the full scan, and which are the latest partial
		 * scans.
		 */
		final List<JSureScan> full = new ArrayList<>();
		// These should end up to be the last in a series
		final Set<JSureScan> roots = new HashSet<>(scans);
		for (JSureScan scan : scans) {
			try {
				final JavaProjectSet<? extends JavaProject> p = scan.getProjects();
				final String lastName = p.getPreviousPartialScan();
				if (lastName != null) {
					// This one is a partial scan and depends on the last one
					final JSureScan last = JSureScan.findByDirName(scans,
							lastName);
					if (last == null) {
						System.err
								.println("Couldn't find previous partial scan: "
										+ last + " -> " + scan.getDirName());
						roots.remove(scan);
					} else {
						// The last scan is not a root
						roots.remove(last);
					}
					scan.setLastPartialScan(last);
				} else {
					full.add(scan);
				}
			} catch (Exception e) {
				// This should never happen
				SLLogger.getLogger().log(Level.SEVERE, I18N.err(179), e);
			}
		}
		List<JSureScan> partials = new ArrayList<>(roots);
		// Sorted: oldest first
		Collections.sort(partials);
		Collections.sort(full);

		Map<JSureScan, JSureScan> fullToPartial = new HashMap<>();
		for (final JSureScan root : partials) {
			// Find the corresponding full run
			JSureScan scan = root;
			while (scan.getLastPartialScan() != null) {
				scan = scan.getLastPartialScan();
			}
			fullToPartial.put(scan, root);
		}
		checkFullScans(full, fullToPartial);

		// Collect which projects map to which scans
		final Map<String, JSureScan> projectToScan = new HashMap<>();
		for (Map.Entry<JSureScan, JSureScan> e : fullToPartial.entrySet()) {
			try {
				final JavaProjectSet<? extends JavaProject> projs = e.getKey().getProjects();
				for (JavaProject p : projs) {
					projectToScan.put(p.getName(), e.getValue());
				}
			} catch (Exception ex) {
				// This should never happen
				SLLogger.getLogger().log(Level.SEVERE, I18N.err(179), e);
			}
		}
		try {
			return new JSureDataDir(dataDir, scans, projectToScan);
		} catch (Exception e) {
			// This should never happen
			SLLogger.getLogger().log(Level.SEVERE, I18N.err(179), e);
		}
		return null;
	}

	/**
	 * Check if all of full has a mapping, and has results.
	 */
	private static void checkFullScans(List<JSureScan> full,
			Map<JSureScan, JSureScan> fullToPartial) {
		for (JSureScan scan : full) {
			if (!fullToPartial.containsKey(scan)) {
				System.out.println("No partials for " + scan);
			}
			try {
				// Check for results
				final File results = new File(scan.getDir(),
						PersistenceConstants.RESULTS_ZIP);
				if (!results.exists() || results.length() == 0) {
					continue;
				}
				// Collect up all the sources
				final Set<String> sources = new HashSet<>();
				for (File src : scan.getSourceZips()) {
					if (src.isFile() && src.getName().endsWith(".zip")) {
						ZipFile zf = new ZipFile(src);
						try {
							Enumeration<? extends ZipEntry> e = zf.entries();
							while (e.hasMoreElements()) {
								ZipEntry ze = e.nextElement();
								String path = ze.getName();
								if (path.endsWith(".java")) {
									sources.add(path);
								}
							}
						} finally {
							zf.close();
						}
					}
				}
				// Check if we have results for each of them
				ZipFile resultsZip = new ZipFile(results);
				try {
					for (String path : sources) {
						if (resultsZip.getEntry(path) == null) {
							//System.out.println("No results for " + path);
						}
					}
				} finally {
					resultsZip.close();
				}
			} catch (Exception e) {
				// This should never happen
				SLLogger.getLogger().log(Level.SEVERE, I18N.err(179), e);
			}
		}
	}
}