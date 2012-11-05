package com.surelogic.javac.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

/**
 * Contains information about what scans exist in a JSure data directory.
 */
public class JSureDataDir {

  @NonNull
  private final File f_dir;

  @NonNull
  private final List<JSureScan> f_scans;

  @NonNull
  private final Map<String, JSureScan> f_projectToScan = new HashMap<String, JSureScan>();

  JSureDataDir(@NonNull File dir, @NonNull List<JSureScan> scans, @NonNull Map<String, JSureScan> projectToScan) throws IOException {
    if (dir == null)
      throw new IllegalArgumentException(I18N.err(44, "dir"));
    f_dir = dir;
    if (scans == null)
      throw new IllegalArgumentException(I18N.err(44, "scans"));
    f_scans = scans;
    if (projectToScan == null)
      throw new IllegalArgumentException(I18N.err(44, "projectToScan"));
    f_projectToScan.putAll(projectToScan);

    for (Map.Entry<String, JSureScan> e : projectToScan.entrySet()) {
      e.getValue().getLatestFilesForProject(e.getKey());
    }
  }

  public boolean isEmpty() {
    return f_scans.isEmpty();
  }

  public File getDir() {
    return f_dir;
  }

  @Nullable
  public synchronized JSureScan findScan(File location) {
    for (JSureScan r : f_scans) {
      if (r.getDir().equals(location)) {
        return r;
      }
    }
    return null;
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
    } catch (Exception e) {
      SLLogger.getLogger().log(Level.WARNING, "Unable to determine projects for " + scan.getDirName(), e);
      return null;
    }
    JSureScan match = null;
    for (JSureScan s : f_scans) {
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
        SLLogger.getLogger().log(Level.WARNING, "Unable to determine projects for " + scan.getDirName(), e);
        continue;
      }
    }
    return match;
  }
}
