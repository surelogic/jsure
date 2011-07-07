package com.surelogic.javac.persistence;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.surelogic.javac.*;

/**
 * Scans and organizes the run directories in the JSure data dir
 * @author Edwin
 */
public class JSureDataDirScanner {
	public static JSureRun findRunDirectory(File f) {
		try {
			return new JSureRun(f);
		} catch (Exception e) {
			// Bad date
			return null;
		}
	}
	
	public static JSureData scan(JSureData oldData) {
		// This redoes everything
		// return scan(oldData.getDataDir());
		return organizeRuns(oldData.getDataDir(), oldData.updateRuns());
	}
	
	public static JSureData scan(File dataDir) {
		final Map<String,JSureRun> runs = new HashMap<String,JSureRun>();
		
		// Look for run directories
		for(File f : dataDir.listFiles()) {
			final JSureRun run = findRunDirectory(f);
		    if (run != null) {
				runs.put(run.getName(), run);				
			}
		}
		return organizeRuns(dataDir, runs);
	}
	
	private static JSureData organizeRuns(File dataDir, Map<String,JSureRun> runs) {	
		// Figure out which are the full runs, and which are the last partial runs
		final List<JSureRun> full = new ArrayList<JSureRun>();
		// These should end up to be the last in a series
		final Set<JSureRun> roots = new HashSet<JSureRun>(runs.values());
		for(JSureRun run : runs.values()) { 
			try {
				final Projects p = run.getProjects();
				final String lastName = p.getLastRun();
				if (lastName != null) {
					// This one is a partial run and depends on the last one
					final JSureRun last = runs.get(lastName);
					if (last == null) {
						System.err.println("Couldn't find run: "+last+" -> "+run.getName());
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
		List<JSureRun> partials = new ArrayList<JSureRun>(roots);
		// Sorted: oldest first
		Collections.sort(partials);
		Collections.sort(full);
		
		Map<JSureRun,JSureRun> fullToPartial = new HashMap<JSureRun, JSureRun>();
		for(final JSureRun root : partials) {
			// Find the corresponding full run 
			JSureRun run = root;
			while (run.getLastRun() != null) {
				run = run.getLastRun();
			}
			fullToPartial.put(run, root);
		}
		checkFullRuns(full, fullToPartial);
		
		// Collect which projects map to which runs?
		final Map<String,JSureRun> project2run = new HashMap<String,JSureRun>();
		for(Map.Entry<JSureRun,JSureRun> e : fullToPartial.entrySet()) {
			try {
				final Projects projs = e.getKey().getProjects();
				for(JavacProject p : projs) {
					project2run.put(p.getName(), e.getValue());
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		try {
			return new JSureData(dataDir, runs, project2run);
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	// Check if all of full has a mapping, and has results
	private static void checkFullRuns(List<JSureRun> full, Map<JSureRun, JSureRun> fullToPartial) {
		for(JSureRun run : full) {
			if (!fullToPartial.containsKey(run)) {
				System.out.println("No partials for "+run);
			}
			try {
				// Check for results
				final File results = new File(run.getDir(), PersistenceConstants.RESULTS_ZIP);
				if (!results.exists()) {
					//System.out.println("No results for full run "+run);
					continue;
				}
				// Collect up all the sources
				final Set<String> sources = new HashSet<String>();
				for(File src : new File(run.getDir(), "zips").listFiles()) {
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
				for(String path : sources) {
					if (resultsZip.getEntry(path) == null) {
						System.out.println("No results for "+path);
					}
				}								
			} catch(Exception e) {
				e.printStackTrace();
			}
		}				
	}
}