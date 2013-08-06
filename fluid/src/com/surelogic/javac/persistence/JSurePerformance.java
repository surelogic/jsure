package com.surelogic.javac.persistence;

import com.surelogic.common.PerformanceProperties;
import com.surelogic.javac.Projects;

public final class JSurePerformance extends PerformanceProperties {
	public static final String PROP_PREFIX = "jsure.";
	
	public JSurePerformance(Projects projects) {
		super(PROP_PREFIX, projects.getLabel(), projects.getRunDir(), ScanProperty.SCAN_PROPERTIES);
	}
}
