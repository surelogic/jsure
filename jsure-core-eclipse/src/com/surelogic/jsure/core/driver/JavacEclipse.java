package com.surelogic.jsure.core.driver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import com.surelogic.analysis.IAnalysisInfo;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.MemoryUtility;
import com.surelogic.javac.Javac;

import edu.cmu.cs.fluid.ide.IDEPreferences;

public class JavacEclipse extends Javac {
  static final JavacEclipse instance = new JavacEclipse();
  
  @Override
  public void initPrefs() {	  
	  setPreference(IDEPreferences.PHYS_MEMORY, MemoryUtility.computePhysMemorySizeInMb());
	  setPreference(IDEPreferences.ANALYSIS_THREAD_COUNT, EclipseUtility.getIntPreference(IDEPreferences.ANALYSIS_THREAD_COUNT));
	  
	  final boolean canRunUniqueness = EclipseUtility.getBooleanPreference(IDEPreferences.SCAN_MAY_RUN_UNIQUENESS);
	  setPreference(IDEPreferences.SCAN_MAY_RUN_UNIQUENESS, canRunUniqueness);
	  for (IAnalysisInfo analysis : getAnalysisInfo()) {
		  final String key = IDEPreferences.ANALYSIS_ACTIVE_PREFIX+analysis.getUniqueIdentifier();
		  boolean pref = EclipseUtility.getBooleanPreference(key) && (canRunUniqueness || !analysis.runsUniqueness());
		  setPreference(key, pref); 
	  }
  }

  public static JavacEclipse getDefault() {
    return instance;
  }

  public static void initialize() {
	  // instance.initPrefs()
  }
  
  public void synchronizeAnalysisPrefs() {
    for (String id : getAvailableAnalyses()) {
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
	  final List<String> m_includedExtensions = new ArrayList<String>();
	  final List<String> m_nonProductionAnalysisExtensions = new ArrayList<String>();
	  for (String id : getAvailableAnalyses()) {
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
    try {
      File f = new File(EclipseUtility.getDirectoryOf("edu.cmu.cs.fluid"));
      return f.toURI().toURL();
    } catch (Throwable e) {
      // Try to use this plugin to find fluid
      String here = EclipseUtility.getDirectoryOf("com.surelogic.jsure.core");
      File f = new File(here);
      System.out.println("j.core = " + f);
      for (File f2 : f.getParentFile().listFiles()) {
        if (f2.getName().startsWith("edu.cmu.cs.fluid_")) {
          System.out.println("Found " + f2);
          try {
            return f2.toURI().toURL();
          } catch (MalformedURLException e1) {
            e1.printStackTrace();
          }
        }
      }
    }
    return null;
  }
}
