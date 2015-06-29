package com.surelogic.jsure.core.driver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.surelogic.analysis.AnalysisDefaults;
import com.surelogic.analysis.IAnalysisInfo;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.MemoryUtility;
import com.surelogic.javac.jobs.JSureConstants;

import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ide.IDERoot;

public class JavacEclipse extends IDERoot {
  static final JavacEclipse instance = new JavacEclipse();

  @Override
  public void initPrefs() {
    setPreference(IDEPreferences.PHYS_MEMORY, MemoryUtility.computePhysMemorySizeInMb());
    setPreference(IDEPreferences.ANALYSIS_THREAD_COUNT, EclipseUtility.getIntPreference(IDEPreferences.ANALYSIS_THREAD_COUNT));

    for (IAnalysisInfo analysis : AnalysisDefaults.getDefault().getAnalysisInfo()) {
      final String key = IDEPreferences.ANALYSIS_ACTIVE_PREFIX + analysis.getUniqueIdentifier();
      boolean pref = EclipseUtility.getBooleanPreference(key);
      setPreference(key, pref);
    }
  }

  public static JavacEclipse getDefault() {
    return instance;
  }

  public static void initialize() {
    // instance.initPrefs()
    if (IDERoot.getInstance() == null) {
      initInstance(instance);
    }
  }

  public void synchronizeAnalysisPrefs() {
    for (String id : AnalysisDefaults.getAvailableAnalyses()) {
      boolean value = EclipseUtility.getBooleanPreference(IDEPreferences.ANALYSIS_ACTIVE_PREFIX + id);
      setPreference(IDEPreferences.ANALYSIS_ACTIVE_PREFIX + id, value);
    }
    for (String pref : IDEPreferences.BOOL_PREFS_TO_SYNC) {
      boolean value = EclipseUtility.getBooleanPreference(pref);
      setPreference(pref, value);
    }
    for (String pref : IDEPreferences.INT_PREFS_TO_SYNC) {
      int value = EclipseUtility.getIntPreference(pref);
      setPreference(pref, value);
    }
    for (String pref : IDEPreferences.STR_PREFS_TO_SYNC) {
      String value = EclipseUtility.getStringPreference(pref);
      setPreference(pref, value);
    }
    /*
     * The data directory is specially set because it is not saved as an Eclipse
     * preference.
     */
    setPreference(IDEPreferences.JSURE_DATA_DIRECTORY, EclipseUtility.getJSureDataDirectory().getAbsolutePath());
  }

  public void writePrefsToXML(File settings) throws FileNotFoundException {
    final List<String> m_includedExtensions = new ArrayList<>();
    final List<String> m_nonProductionAnalysisExtensions = new ArrayList<>();
    for (String id : AnalysisDefaults.getAvailableAnalyses()) {
      boolean value = EclipseUtility.getBooleanPreference(IDEPreferences.ANALYSIS_ACTIVE_PREFIX + id);
      if (value) {
        m_includedExtensions.add(id);
      } else {
        m_nonProductionAnalysisExtensions.add(id);
      }
    }
    // Modified from DoubleChecker
    final PrintWriter pw = new PrintWriter(settings);
    pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    pw.println("<preferences>");
    pw.println("  <included-analysis-modules>");
    for (String id : m_includedExtensions) {
      pw.println("    <id>" + id + "</id>");
    }
    pw.println("  </included-analysis-modules>");
    pw.println("  <excluded-analysis-modules>");
    for (String id : m_nonProductionAnalysisExtensions) {
      pw.println("    <id>" + id + "</id>");
    }
    pw.println("  </excluded-analysis-modules>");
    pw.println("</preferences>");
    pw.close();
  }

  @Override
  public URL getResourceRoot() {
    return EclipseUtility.getInstallationURLOf(JSureConstants.JSURE_ANALYSIS_PLUGIN_ID);
  }

  @Override
  public boolean getBooleanPreference(String key) {
    /*
     * if (testing && IDEPreferences.ALLOW_JAVADOC_ANNOS.equals(key)) { //
     * Enable, so we can test it! return true; }
     */
    return EclipseUtility.getBooleanPreference(key);
  }

  @Override
  public int getIntPreference(String key) {
    return EclipseUtility.getIntPreference(key);
  }

  @Override
  public String getStringPreference(String key) {
    return EclipseUtility.getStringPreference(key);
  }
}
