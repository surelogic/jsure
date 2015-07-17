package com.surelogic.javac.persistence;

import com.surelogic.analysis.ConcurrentAnalysis;
import com.surelogic.common.PerformanceProperties;
import com.surelogic.java.persistence.ScanProperty;
import com.surelogic.javac.Projects;

public final class JSurePerformance extends PerformanceProperties {
  public static final String PROP_PREFIX = "jsure.";

  public final boolean singleThreaded;

  public JSurePerformance(Projects projects, boolean singleThreaded) {
    super(PROP_PREFIX, projects.getLabel(), projects.getRunDir(), ScanProperty.SCAN_PROPERTIES);
    this.singleThreaded = singleThreaded;
    this.setIntProperty("num.threads", singleThreaded ? 1 : ConcurrentAnalysis.getThreadCountToUse());
  }
}
