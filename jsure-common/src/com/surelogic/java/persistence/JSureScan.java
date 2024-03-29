package com.surelogic.java.persistence;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.FileUtility;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.java.Config;
import com.surelogic.common.java.IJavaFactory;
import com.surelogic.common.java.JavaProject;
import com.surelogic.common.java.JavaProjectSet;
import com.surelogic.common.java.JavaProjectsXMLReader;
import com.surelogic.common.java.PersistenceConstants;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.remote.RemoteScanJob;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.common.tool.IResults;

import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ide.IDERoot;

public class JSureScan implements Comparable<JSureScan>, IResults {
  public static final String RESULTS_XML = "sea_snapshot.xml";
  public static final String RESULTS_XML_COMPRESSED = RESULTS_XML + FileUtility.GZIP_SUFFIX;
  private static final String TEMP_RESULTS_XML = RemoteScanJob.TEMP_PREFIX + RESULTS_XML;
  private static final String COMPRESSED_TEMP_RESULTS_XML = RemoteScanJob.TEMP_PREFIX + RESULTS_XML + FileUtility.GZIP_SUFFIX;

  /**
   * As a double
   */
  private static final ScanProperty<JSureScan> SIZE_IN_MB = new ScanProperty<JSureScan>("scan.size.in.mb") {
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
    String computeValue(JSureScan s) {
      final double size = FileUtility.recursiveSizeInBytes(s.getDir()) / (1024 * 1024.0);
      return Double.toString(size);
    }
  };

  /**
   * As a comma-separated list
   */
  private static final ScanProperty<JSureScan> SCANNED_PROJECTS = new ScanProperty<JSureScan>("scanned.projects") {
    @Override
    String computeValue(JSureScan s) {
      try {
        return s.getProjects().getLabel();
      } catch (Exception e) {
        return null;
      }
    }
  };

  @SuppressWarnings("unchecked")
  private static final List<ScanProperty<JSureScan>> REQUIRED_PROPS = SLUtility.list(SIZE_IN_MB, SCANNED_PROJECTS);

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

  private static String[] requiredFiles = {
      // RemoteJSureRun.LOG_TXT,
      PersistenceConstants.PROJECTS_XML };

  public static boolean isValidScan(final File dir) {
    if (!doesDirNameFollowScanNamingConventions(dir.getName())) {
      return false;
    }
    for (String required : requiredFiles) {
      final File r = new File(dir, required);
      if (!r.isFile()) {
        return false;
      }
    }
    // Check for results
    final File results = findResultsXML(dir);
    return results.isFile();
  }

  /**
   * Finds the results file in the scan directory. Note that the returned file
   * may not exist.
   * 
   * @param scanDir
   *          a JSure scan directory.
   * @return a results file which may or may not exist.
   */
  @NonNull
  public static File findResultsXML(File scanDir) {
    // only return uncompressed if it exists
    final File rv = new File(scanDir, RESULTS_XML);
    if (rv.isFile())
      return rv;
    return new File(scanDir, RESULTS_XML_COMPRESSED);
  }

  /**
   * Finds the log file in the scan directory. Note that the returned file may
   * not exist.
   * 
   * @param scanDir
   *          a JSure scan directory.
   * @return a log file which may or may not exist.
   */
  @NonNull
  public static File findLogFile(File scanDir) {
    return new File(scanDir, SLUtility.LOG_NAME);
  }

  public static boolean isIncompleteScan(final File dir) {
    if (!doesDirNameFollowScanNamingConventions(dir.getName())) {
      return false;
    }
    return !findResultsXML(dir).isFile();
  }

  /**
   * Checks if the passed directory name appears to follow the conventions for
   * scan directories. If so, the name ends with a date.
   * 
   * @param scanDirName
   *          the directory name to check.
   * @return {@code true} if the date at the end of <tt>scanDirName</tt> can be
   *         parsed, {@code false} otherwise.
   */
  public static boolean doesDirNameFollowScanNamingConventions(String scanDirName) {
    Date result = SLUtility.getDateFromScanDirectoryNameOrNull(scanDirName);
    if (result != null)
      return true;
    else
      // TODO REMOVE OLD OLD OLD WHEN REGRESSIONS UPDATED
      return RegressionUtility.extractDateFromName(scanDirName) != null;
  }

  @NonNull
  private final Date f_timeOfScan;
  @NonNull
  private final File f_scanDir;
  private JavaProjectSet<? extends JavaProject> f_projectsScanned;
  private JSureScan f_lastPartialScan;
  private final double f_sizeInMB; // non-null

  public JSureScan(File scanDir) throws Exception {
    if (scanDir == null || !scanDir.isDirectory()) {
      throw new IllegalArgumentException(I18N.err(44, "scanDir"));
    }
    f_scanDir = scanDir;

    // Extract the time of the scan
    Date time = SLUtility.getDateFromScanDirectoryNameOrNull(scanDir.getName());
    if (time == null) {
      // try old scheme
      // GET RID OF WHEN ALL REGRESSIONS UPDATED
      // OLD OLD OLD
      time = RegressionUtility.extractDateFromName(scanDir.getName());
    }
    if (time == null) {
      throw new IllegalArgumentException(I18N.err(229, scanDir.getName()));
    }
    f_timeOfScan = time;

    final Properties props = ScanProperty.getScanProperties(scanDir, this, REQUIRED_PROPS);
    f_sizeInMB = Double.parseDouble(props.getProperty(SIZE_IN_MB.key));

    // check the various files
    getProjects();
  }

  public Date getTimeOfScan() {
    return f_timeOfScan;
  }

  public File getDir() {
    return f_scanDir;
  }

  @NonNull
  public File getLogFile() {
    return findLogFile(f_scanDir);
  }

  @NonNull
  public File getResultsFile() {
    return findResultsXML(f_scanDir);
  }

  @NonNull
  public String getDirName() {
    return f_scanDir.getName();
  }

  public double getSizeInMB() {
    return f_sizeInMB;
  }

  public synchronized final JavaProjectSet<? extends JavaProject> getProjects() throws Exception {
    if (f_projectsScanned != null) {
      return f_projectsScanned;
    }
    // Get info about projects
    Reader reader = new Reader();
    f_projectsScanned = reader.readProjectsXML(f_scanDir);
    if (f_projectsScanned != null) {
      f_projectsScanned.setMonitor(new NullSLProgressMonitor());
      f_projectsScanned.setScanDir(f_scanDir);
      if (!f_projectsScanned.getRunDir().equals(f_scanDir)) {
        throw new IllegalStateException();
      }
    }
    return f_projectsScanned;
  }

  static class Reader extends JavaProjectsXMLReader<JavaProject> {
    public Reader() {
      super(IJavaFactory.prototype);
    }

    @Override
    protected void setupDefaultJRE(String projectName) {
      if (projectName.startsWith(Config.JRE_NAME)) {
        // TODO what if this should be JavacEclipse?

        // Javac.getDefault().setPreference(IDEPreferences.DEFAULT_JRE,
        // projectName);
      }
    }
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

  @Override
  public int compareTo(JSureScan o) {
    return f_timeOfScan.compareTo(o.f_timeOfScan);
  }

  @Override
  public String toString() {
    if (f_lastPartialScan != null) {
      return "JSureScan: " + f_scanDir.getName() + " ->\n\t" + f_lastPartialScan;
    }
    return "JSureScan: " + f_scanDir.getName();
  }

  public Map<String, JSureFileInfo> getLatestFilesForProject(String proj) throws IOException {
    if (proj.startsWith(Config.JRE_NAME)) {
      return Collections.emptyMap();
    }
    final File srcZip = new File(getSourceZipsDir(), proj + ".zip");
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
      info = new HashMap<>();
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

  /**
   * Gets the sourcs zips directory.
   * 
   * @return a directory, or {@code null} if none exits.
   */
  @Nullable
  private File getSourceZipsDir() {
    File dir = new File(f_scanDir, PersistenceConstants.ZIPS_DIR);
    if (dir.isDirectory())
      return dir;
    return null;
  }

  public Iterable<File> getSourceZips() {
    final File dir = getSourceZipsDir();
    if (!dir.isDirectory()) {
      return Collections.emptyList();
    }
    return Arrays.asList(dir.listFiles());
  }

  void clear() {
    f_projectsScanned.clear();
  }

  public static File getResultsFile(File scanDir) {
    final boolean compress = IDERoot.getInstance().getBooleanPreference(IDEPreferences.SCAN_MAY_USE_COMPRESSION);
    final File location = new File(scanDir, compress ? COMPRESSED_TEMP_RESULTS_XML : TEMP_RESULTS_XML);
    return location;
  }

  public static void markAsRunning(File scanDir) {
    final File location = getResultsFile(scanDir);
    try {
      location.createNewFile();
    } catch (IOException e) {
      // Ignore
    }
  }

  public String getLabel() {
    return getDir().getName();
  }
}
