package com.surelogic.javac.jobs;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.util.QuickProperties;

public final class JSureConstants {
  public static final Logger LOG = SLLogger.getLogger("JSURE");

  public static final QuickProperties.Flag versioningFlag = new QuickProperties.Flag(LOG, "fluid.ir.versioning", "Versioning", false, true);

  public static boolean versioningIsOn = versioningIsOn();

  public static boolean versioningIsOn() {
    return QuickProperties.checkFlag(versioningFlag);
  }

  /** Used for Ant and Maven scans */
  public static final String JSURE_SCAN_TASK_SUFFIX = ".jsure-scan.zip";

  public static final String JSURE_COMMON_PLUGIN_ID = "com.surelogic.jsure.common";
  public static final String JSURE_ANALYSIS_PLUGIN_ID = "com.surelogic.jsure.analysis";
  public static final String JSURE_TESTS_PLUGIN_ID = "com.surelogic.jsure.tests";
  public static final String JUNIT_PLUGIN_ID = "org.junit";
}
