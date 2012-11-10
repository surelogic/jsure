package com.surelogic.javac.persistence;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.surelogic.common.FileUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.javac.JavacTypeEnvironment;
import com.surelogic.javac.Projects;
import com.surelogic.javac.jobs.RemoteJSureRun;

public class JSureScan implements Comparable<JSureScan> {
  public static final String INCOMPLETE_SCAN = "running_or_crashed";
  public static final String COMPLETE_SCAN = "complete";
  public static final String OLD_COMPLETE_SCAN = "Complete.Scan";
  private static final String SCAN_PROPERTIES = "scan.properties";

  private abstract static class ScanProperty {
    final String key;

    ScanProperty(String k) {
      key = k;
    }

    boolean isValid(String value) {
      return value != null;
    }

    abstract String computeValue(JSureScan s);
  }

  /**
   * As a double
   */
  private static final ScanProperty SIZE_IN_MB = new ScanProperty("scan.size.in.mb") {
    @Override
    boolean isValid(String value) {
      if (super.isValid(value)) {
        try {
          double d = Double.parseDouble(value);
          return d > 0;
        } catch (NumberFormatException e) {
          return false;
        }
      }
      return false;
    }

    @Override
    public String computeValue(JSureScan s) {
      final double size = FileUtility.recursiveSizeInBytes(s.getDir()) / (1024 * 1024.0);
      return Double.toString(size);
    }
  };

  private static final ScanProperty[] REQUIRED_PROPS = { SIZE_IN_MB, };

  /**
   * Looks up a scan by its directory name in a list of scans.
   * 
   * @param in
   *          the collection of scans to search.
   * @param dirName
   *          the directory name of the desired scan.
   * @return the desired scan, or {@code null} if it cannot be found.
   */
  public static JSureScan findByDirName(List<JSureScan> in, String dirName) {
    for (JSureScan scan : in) {
      if (scan.getDirName().equals(dirName))
        return scan;
    }
    return null;
  }

  private static String[] requiredFiles = { JSureScan.COMPLETE_SCAN, RemoteJSureRun.SUMMARIES_ZIP, RemoteJSureRun.RESULTS_XML,
      RemoteJSureRun.LOG_TXT, PersistenceConstants.PROJECTS_XML };

  public static boolean isValidScan(final File dir) {
    if (!doesDirNameFollowScanNamingConventions(dir.getName())) {
      return false;
    }
    for (String required : requiredFiles) {
      final File r = new File(dir, required);
      if (!r.isFile()) {
        if (required.equals(COMPLETE_SCAN)) {
          // check for old file name
          if (new File(dir, OLD_COMPLETE_SCAN).isFile())
            continue;
        }
        return false;
      }
    }
    return true;
  }

  public static boolean isIncompleteScan(final File dir) {
    if (!doesDirNameFollowScanNamingConventions(dir.getName())) {
      return false;
    }
    return new File(dir, INCOMPLETE_SCAN).isFile();
  }

  /**
   * Checks if the passed directory name appears to follow the conventions for
   * scan directories. If so, the name should have at least three segments:
   * label, date, and time.
   * <p>
   * This check sees if there are at least three segments, then tries to parse
   * the date and time. If this all works {@code true} is returned.
   * 
   * @param dirName
   *          the directory name to check.
   * @return {@code true} if there are at least three segments in
   *         <tt>dirName</tt> and the date and time parse, {@code false}
   *         otherwise.
   */
  public static boolean doesDirNameFollowScanNamingConventions(String dirName) {
    return RegressionUtility.extractDateFromName(dirName) != null;
  }

  private final Date f_timeOfScan; // non-null
  private final File f_scanDir; // non-null
  private Projects f_projectsScanned;
  private JSureScan f_lastPartialScan;
  private final double f_sizeInMB; // non-null

  public JSureScan(File scanDir) throws Exception {
    if (scanDir == null || !scanDir.isDirectory()) {
      throw new IllegalArgumentException();
    }
    f_scanDir = scanDir;

    // There should be at least 3 segments: label date time
    final Date time = RegressionUtility.extractDateFromName(scanDir.getName());
    if (time == null) {
      throw new IllegalArgumentException(I18N.err(229, scanDir.getName()));
    }
    f_timeOfScan = time;

    final Properties props = getScanProperties(scanDir);
    f_sizeInMB = Double.parseDouble(props.getProperty(SIZE_IN_MB.key));

    // check the various files
    getProjects();
  }

  /**
   * Returns all the expected properties
   */
  private Properties getScanProperties(File scanDir) {
    final Properties props = new Properties();
    final File precomputed = new File(scanDir, SCAN_PROPERTIES);
    final boolean alreadyPrecomputed = precomputed.exists();
    if (alreadyPrecomputed) {
      InputStream in = null;
      try {
        in = new FileInputStream(precomputed);
        props.load(in);
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    // Check if I have all the info that I need
    boolean changed = false;
    for (ScanProperty p : REQUIRED_PROPS) {
      if (!p.isValid(props.getProperty(p.key))) {
        props.setProperty(p.key, p.computeValue(this));
        changed = true;
      }
    }

    final File completed = new File(scanDir, COMPLETE_SCAN);
    if (completed.exists()) {
      if (alreadyPrecomputed) {
        final long pMod = precomputed.lastModified();
        final long cMod = completed.lastModified();
        changed = pMod <= cMod;
        if (changed) {
          System.out.println("Changed");
        }
      }
    } else {
      // Don't write it out if it's not done yet
      changed = false;
    }
    if (changed) {
      // Rewrite the properties file
      OutputStream out = null;
      try {
        out = new FileOutputStream(precomputed);
        props.store(out, "Precomputed info for JSureScan");
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (out != null) {
          try {
            out.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    return props;
  }

  public Date getTimeOfScan() {
    return f_timeOfScan;
  }

  public File getDir() {
    return f_scanDir;
  }

  public File getResultsFile() {
    return new File(f_scanDir, RemoteJSureRun.RESULTS_XML);
  }

  public String getDirName() {
    return f_scanDir.getName();
  }

  public double getSizeInMB() {
    return f_sizeInMB;
  }

  public synchronized final Projects getProjects() throws Exception {
    if (f_projectsScanned != null) {
      return f_projectsScanned;
    }
    // Get info about projects
    JSureProjectsXMLReader reader = new JSureProjectsXMLReader();
    reader.read(new File(f_scanDir, PersistenceConstants.PROJECTS_XML));
    f_projectsScanned = reader.getProjects();
    f_projectsScanned.setMonitor(NullSLProgressMonitor.getFactory().createSLProgressMonitor(""));
    return f_projectsScanned;
  }

  public void setLastPartialScan(JSureScan last) {
    if (f_lastPartialScan != null && f_lastPartialScan != last || last == null) {
      throw new IllegalArgumentException();
    }
    f_lastPartialScan = last;
  }

  public JSureScan getLastPartialScan() {
    return f_lastPartialScan;
  }

  public int compareTo(JSureScan o) {
    return f_timeOfScan.compareTo(o.f_timeOfScan);
  }

  public String toString() {
    if (f_lastPartialScan != null) {
      return "JSureScan: " + f_scanDir.getName() + " ->\n\t" + f_lastPartialScan;
    }
    return "JSureScan: " + f_scanDir.getName();
  }

  public Map<String, JSureFileInfo> getLatestFilesForProject(String proj) throws IOException {
    if (proj.startsWith(JavacTypeEnvironment.JRE_NAME)) {
      return Collections.emptyMap();
    }
    final File srcZip = new File(f_scanDir, PersistenceConstants.ZIPS_DIR + '/' + proj + ".zip");
    if (!srcZip.exists()) {
      // throw new IllegalStateException("No sources: "+srcZip);
      System.err.println("No sources: " + srcZip);
      return Collections.emptyMap();
    }

    final Map<String, JSureFileInfo> info;
    final File resultsZip;
    if (f_lastPartialScan != null) {
      resultsZip = new File(f_scanDir, PersistenceConstants.PARTIAL_RESULTS_ZIP);
      info = f_lastPartialScan.getLatestFilesForProject(proj);
    } else {
      resultsZip = new File(f_scanDir, PersistenceConstants.RESULTS_ZIP);
      info = new HashMap<String, JSureFileInfo>();
    }
    if (!resultsZip.exists() || resultsZip.length() == 0) {
      // System.out.println("No results: "+resultsZip);
      return Collections.emptyMap();
    }
    // Overwrite any changes from results
    final JSureFileInfo thisInfo = new JSureFileInfo(srcZip, resultsZip);
    ZipFile zip = new ZipFile(resultsZip);
    try {
      Enumeration<? extends ZipEntry> e = zip.entries();
      while (e.hasMoreElements()) {
        ZipEntry ze = e.nextElement();
        JSureFileInfo old = info.put(ze.getName(), thisInfo);
        if (old != null) {
          System.out.println("Replacing " + ze.getName() + " with entry from " + old);
        }
      }
    } finally {
      zip.close();
    }
    return info;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((f_scanDir == null) ? 0 : f_scanDir.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    JSureScan other = (JSureScan) obj;
    if (f_scanDir == null) {
      if (other.f_scanDir != null)
        return false;
    } else if (!f_scanDir.equals(other.f_scanDir))
      return false;
    return true;
  }

  public Iterable<File> getSourceZips() {
    return Arrays.asList(new File(f_scanDir, PersistenceConstants.ZIPS_DIR).listFiles());
  }
}
