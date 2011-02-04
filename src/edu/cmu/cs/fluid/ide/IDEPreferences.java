package edu.cmu.cs.fluid.ide;

public interface IDEPreferences {
	String PREFIX = "edu.cmu.cs.fluid.";
	String ALLOW_JAVADOC_ANNOS = PREFIX + "allow.javadoc.annos";
	String ANALYSIS_THREAD_COUNT = PREFIX + "analysis.thread.count";
	String JSURE_DATA_DIRECTORY = PREFIX + "data.directory";
	String DEFAULT_JRE = PREFIX + "default.JRE";

	/**
	 * The preference prefix for whether an analysis is on
	 */
	String ANALYSIS_ACTIVE_PREFIX = "com.surelogic.jsure.active.";
}
