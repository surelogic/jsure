package com.surelogic.javac.persistence;

import java.util.concurrent.*;

import com.surelogic.analysis.ConcurrentAnalysis;
import com.surelogic.common.PerformanceProperties;
import com.surelogic.java.persistence.ScanProperty;
import com.surelogic.javac.Projects;

import extra166y.ParallelArray;

public final class JSurePerformance extends PerformanceProperties {
	public static final String PROP_PREFIX = "jsure.";
	
	public final boolean singleThreaded;
	public final ForkJoinPool pool;
	
	public JSurePerformance(Projects projects, boolean singleThreaded) {
		super(PROP_PREFIX, projects.getLabel(), projects.getRunDir(), ScanProperty.SCAN_PROPERTIES);
		this.singleThreaded = singleThreaded;
	    this.setIntProperty("num.threads", singleThreaded ? 1 : ConcurrentAnalysis.threadCount);
	    pool = singleThreaded ? new ForkJoinPool(1) : ConcurrentAnalysis.pool;
	}
	
	public <T> ParallelArray<T> createArray(Class<T> cls) {
		return ParallelArray.create(0, cls, pool);
	}
}
