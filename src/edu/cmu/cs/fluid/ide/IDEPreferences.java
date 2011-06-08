package edu.cmu.cs.fluid.ide;

public interface IDEPreferences {
	String PREFIX = "edu.cmu.cs.fluid.";
	String TOOL_MEMORY_MB = PREFIX + "tool-memory-mb";
	String ALLOW_JAVADOC_ANNOS = PREFIX + "allow.javadoc.annos";
	String ANALYSIS_THREAD_COUNT = PREFIX + "analysis.thread.count";
	String JSURE_DATA_DIRECTORY = PREFIX + "data.directory";
	String DEFAULT_JRE = PREFIX + "default.JRE";
	String JSURE_XML_DIRECTORY = PREFIX + "xml.directory";

	String TIMEOUT_WARNING_SEC = PREFIX + "timeoutWarningSec";
	String TIMEOUT_FLAG = PREFIX + "timeoutFlag";
	String TIMEOUT_SEC = PREFIX + "timeoutSec";

	/**
	 * The preference prefix for whether an analysis is on
	 */
	String ANALYSIS_ACTIVE_PREFIX = "com.surelogic.jsure.active.";
}
