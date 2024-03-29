package edu.cmu.cs.fluid.ide;

public interface IDEPreferences {
  String PREFIX = "edu.cmu.cs.fluid.";
  String TOOL_MEMORY_MB = PREFIX + "tool-memory-mb";
  String ANALYSIS_THREAD_COUNT = PREFIX + "analysis.thread.count";
  String JSURE_DATA_DIRECTORY = PREFIX + "data.directory";
  String DEFAULT_JRE = PREFIX + "default.JRE";
  String JSURE_XML_DIFF_DIRECTORY = PREFIX + "xml.diff.directory";
  String SCAN_MAY_USE_COMPRESSION = PREFIX + "scan.may.use.compression";
  String LOAD_ALL_CLASSES = PREFIX + "load.all.classes";
  String UNINTERESTING_PACKAGE_FILTERS = PREFIX + "modeling.problem.filters";
  String MAKE_NONABDUCTIVE_PROPOSALS = PREFIX + "make.nonabductive.proposals";

  String TIMEOUT_WARNING_SEC = PREFIX + "timeoutWarningSec";
  String TIMEOUT_FLAG = PREFIX + "timeoutFlag";
  String TIMEOUT_SEC = PREFIX + "timeoutSec";
  int DEFAULT_TIMEOUT_SEC = 60; // seconds

  String PHYS_MEMORY = PREFIX + "physMemoryInMB";

  String[] BOOL_PREFS_TO_SYNC = { LOAD_ALL_CLASSES, SCAN_MAY_USE_COMPRESSION, TIMEOUT_FLAG, MAKE_NONABDUCTIVE_PROPOSALS };

  String[] INT_PREFS_TO_SYNC = { TOOL_MEMORY_MB, ANALYSIS_THREAD_COUNT, TIMEOUT_WARNING_SEC, TIMEOUT_SEC, };

  /*
   * Note that JSURE_DATA_DIRECTORY is handled as a special case, it should not
   * be in the list below.
   */
  String[] STR_PREFS_TO_SYNC = { JSURE_XML_DIFF_DIRECTORY, UNINTERESTING_PACKAGE_FILTERS };

  /**
   * The preference prefix for whether an analysis is on
   */
  String ANALYSIS_ACTIVE_PREFIX = "com.surelogic.jsure.active.";
}
