package com.surelogic.jsure.core.preferences;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import com.surelogic.NonNull;
import com.surelogic.analysis.*;
import com.surelogic.common.FileUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.MemoryUtility;
import com.surelogic.common.core.preferences.AutoPerspectiveSwitchPreferences;

import edu.cmu.cs.fluid.ide.IDEPreferences;

/**
 * Defines preference constants for the JSure tool.
 * <p>
 * The preferences are manipulated using the API in {@link EclipseUtility}.
 * Eclipse UI code that uses an <tt>IPreferenceStore</tt> may obtain one that
 * accesses the JSure preferences by calling
 * <tt>EclipseUIUtility.getPreferences()</tt>.
 */
public final class JSurePreferencesUtility {

  private static final String PREFIX = "com.surelogic.jsure.core.";

  private static final AtomicBoolean f_initializationNeeded = new AtomicBoolean(true);

  /**
   * Sets up the default values for the JSure tool.
   * <p>
   * <b>WARNING:</b> Because this class exports strings that are declared to be
   * {@code public static final} simply referencing these constants may not
   * trigger Eclipse to load the containing plug-in. This is because the
   * constants are copied by the Java compiler into using class files. This
   * means that each using plug-in <b>must</b> invoke
   * {@link #initializeDefaultScope()} in its plug-in activator's {@code start}
   * method.
   */
  public static void initializeDefaultScope() {
    if (f_initializationNeeded.compareAndSet(true, false)) {

      int cpuCount = Runtime.getRuntime().availableProcessors();
      if (cpuCount < 1)
        cpuCount = 1;
      EclipseUtility.setDefaultIntPreference(IDEPreferences.ANALYSIS_THREAD_COUNT, cpuCount);

      // Underestimate a little bit
      final int estimatedMax = MemoryUtility.computeMaxMemorySizeInMb() - 256;
      int mem = 2048;
      while (mem > estimatedMax) {
        mem -= 512;
      }
      EclipseUtility.setDefaultIntPreference(IDEPreferences.TOOL_MEMORY_MB, mem);
      EclipseUtility.setDefaultBooleanPreference(IDEPreferences.LOAD_ALL_CLASSES, false);

      EclipseUtility.setDefaultStringListPreference(IDEPreferences.UNINTERESTING_PACKAGE_FILTERS,
          UninterestingPackageFilterUtility.DEFAULT);

      EclipseUtility.setDefaultBooleanPreference(SHOW_BALLOON_NOTIFICATIONS, true);
      EclipseUtility.setDefaultBooleanPreference(getSwitchPreferences().getPromptPerspectiveSwitchConstant(), true);
      EclipseUtility.setDefaultBooleanPreference(getSwitchPreferences().getAutoPerspectiveSwitchConstant(), true);
      EclipseUtility.setDefaultBooleanPreference(ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_SCAN, true);
      EclipseUtility.setDefaultBooleanPreference(ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_UPDATE_JAR, true);
      EclipseUtility.setDefaultBooleanPreference(IDEPreferences.SCAN_MAY_USE_COMPRESSION, true);

      EclipseUtility.setDefaultStringPreference(REGION_MODEL_NAME_SUFFIX, "State");
      EclipseUtility.setDefaultBooleanPreference(REGION_MODEL_NAME_CAP, true);
      EclipseUtility.setDefaultBooleanPreference(REGION_MODEL_NAME_COMMON_STRING, true);
      EclipseUtility.setDefaultStringPreference(LOCK_MODEL_NAME_SUFFIX, "Lock");
      EclipseUtility.setDefaultBooleanPreference(LOCK_MODEL_NAME_CAP, true);

      final File jsureDataDir = EclipseUtility.getJSureDataDirectory();
      File xmlDiffDir = new File(jsureDataDir, FileUtility.JSURE_XML_DIFF_PATH_FRAGMENT);
      EclipseUtility.setDefaultStringPreference(IDEPreferences.JSURE_XML_DIFF_DIRECTORY, xmlDiffDir.getAbsolutePath());

      for (IAnalysisInfo a : AnalysisDefaults.getDefault().getAnalysisInfo()) {
        // System.out.println("Defaulting "+a.getUniqueIdentifier()+" to "+a.isProduction());
        EclipseUtility.setDefaultBooleanPreference(IDEPreferences.ANALYSIS_ACTIVE_PREFIX + a.getUniqueIdentifier(),
            a.isProduction());
      }

      EclipseUtility.setDefaultIntPreference(IDEPreferences.TIMEOUT_WARNING_SEC, 30);
      EclipseUtility.setDefaultBooleanPreference(IDEPreferences.TIMEOUT_FLAG, true);
      EclipseUtility.setDefaultIntPreference(IDEPreferences.TIMEOUT_SEC, IDEPreferences.DEFAULT_TIMEOUT_SEC);

      EclipseUtility.setDefaultBooleanPreference(SAVE_DIRTY_EDITORS_BEFORE_VERIFY, false);

      EclipseUtility.setDefaultBooleanPreference(VSTATUS_SHOW_HINTS, true);
      EclipseUtility.setDefaultBooleanPreference(VSTATUS_HIGHLIGHT_DIFFERENCES, true);
      EclipseUtility.setDefaultIntPreference(VSTATUS_TREE_WIDTH, 300);
      EclipseUtility.setDefaultIntPreference(VSTATUS_PROJECT_WIDTH, 100);
      EclipseUtility.setDefaultIntPreference(VSTATUS_PACKAGE_WIDTH, 100);
      EclipseUtility.setDefaultIntPreference(VSTATUS_TYPE_WIDTH, 100);
      EclipseUtility.setDefaultIntPreference(VSTATUS_LINE_WIDTH, 40);
      EclipseUtility.setDefaultIntPreference(VSTATUS_COL_DIFF_WIDTH, 200);

      EclipseUtility.setDefaultBooleanPreference(VEXPLORER_HIGHLIGHT_DIFFERENCES, true);
      EclipseUtility.setDefaultBooleanPreference(VEXPLORER_SHOW_OBSOLETE_DROP_DIFFERENCES, false);
      EclipseUtility.setDefaultBooleanPreference(VEXPLORER_SHOW_ONLY_DIFFERENCES, false);
      EclipseUtility.setDefaultBooleanPreference(VEXPLORER_SHOW_ONLY_DERIVED_FROM_SRC, true);
      EclipseUtility.setDefaultBooleanPreference(VEXPLORER_SHOW_ANALYSIS_RESULTS, false);
      EclipseUtility.setDefaultBooleanPreference(VEXPLORER_SHOW_HINTS, false);
      EclipseUtility.setDefaultIntPreference(VEXPLORER_COL_TREE_WIDTH, 300);
      EclipseUtility.setDefaultIntPreference(VEXPLORER_COL_POSITION_WIDTH, 100);
      EclipseUtility.setDefaultIntPreference(VEXPLORER_COL_LINE_WIDTH, 40);
      EclipseUtility.setDefaultIntPreference(VEXPLORER_COL_DIFF_WIDTH, 200);

      EclipseUtility.setDefaultBooleanPreference(PROPOSED_ANNOTATIONS_SHOW_ABDUCTIVE_ONLY, true);
      EclipseUtility.setDefaultIntPreference(PROPOSED_ANNO_COL_TREE_WIDTH, 300);
      EclipseUtility.setDefaultIntPreference(PROPOSED_ANNO_COL_LINE_WIDTH, 40);
      EclipseUtility.setDefaultBooleanPreference(PROPOSED_ANNO_HIGHLIGHT_DIFFERENCES, true);

      EclipseUtility.setDefaultBooleanPreference(PROBLEMS_HIGHLIGHT_DIFFERENCES, true);

      EclipseUtility.setDefaultBooleanPreference(VIEWS_SAVE_TREE_STATE, true);

      EclipseUtility.setDefaultBooleanPreference(METRIC_ALPHA_SORT, true);

      EclipseUtility.setDefaultIntPreference(METRIC_VIEW_SLOC_THRESHOLD, 1000);
      EclipseUtility.setDefaultIntPreference(METRIC_SLOC_SASH_LHS_WEIGHT, 60);
      EclipseUtility.setDefaultIntPreference(METRIC_SLOC_SASH_RHS_WEIGHT, 40);
      EclipseUtility.setDefaultIntPreference(METRIC_SLOC_COL_TREE_WIDTH, 300);
      EclipseUtility.setDefaultIntPreference(METRIC_SLOC_COL_BLANK_LINE_COUNT_WIDTH, 80);
      EclipseUtility.setDefaultIntPreference(METRIC_SLOC_COL_CONTAINS_COMMENT_LINE_COUNT_WIDTH, 80);
      EclipseUtility.setDefaultIntPreference(METRIC_SLOC_COL_JAVA_DECLARATION_COUNT_WIDTH, 80);
      EclipseUtility.setDefaultIntPreference(METRIC_SLOC_COL_JAVA_STATEMENT_COUNT_WIDTH, 80);
      EclipseUtility.setDefaultIntPreference(METRIC_SLOC_COL_LINE_COUNT_WIDTH, 80);
      EclipseUtility.setDefaultIntPreference(METRIC_SLOC_COL_SEMICOLON_COUNT_WIDTH, 80);
      EclipseUtility.setDefaultIntPreference(METRIC_VIEW_SLOC_COMBO_SELECTED_COLUMN, 0);
      EclipseUtility.setDefaultBooleanPreference(METRIC_VIEW_SLOC_THRESHOLD_SHOW_ABOVE, true);

      EclipseUtility.setDefaultIntPreference(METRIC_SCAN_TIME_COL_TREE_WIDTH, 300);
      EclipseUtility.setDefaultIntPreference(METRIC_SCAN_TIME_COL_DURATION_WIDTH, 80);
      EclipseUtility.setDefaultStringPreference(METRIC_SCAN_TIME_ANALYSIS_TO_SHOW, "");
      EclipseUtility.setDefaultIntPreference(METRIC_SCAN_TIME_THRESHOLD_MS, 500);
      EclipseUtility.setDefaultBooleanPreference(METRIC_SCAN_TIME_THRESHOLD_SHOW_ABOVE, true);

      EclipseUtility.setDefaultIntPreference(METRIC_DROP_COUNTER_COL_DROP_WIDTH, 300);
      EclipseUtility.setDefaultIntPreference(METRIC_DROP_COUNTER_COL_COUNT_WIDTH, 100);

      EclipseUtility.setDefaultIntPreference(METRIC_VIEW_STATEWRT_COMBO_SELECTED_COLUMN, 0);
      EclipseUtility.setDefaultBooleanPreference(METRIC_VIEW_STATEWRT_THRESHOLD_SHOW_ABOVE, true);
      EclipseUtility.setDefaultIntPreference(METRIC_VIEW_STATEWRT_THRESHOLD, 10);
      EclipseUtility.setDefaultIntPreference(METRIC_VIEW_STATEWRT_TREE_WIDTH, 300);
      EclipseUtility.setDefaultIntPreference(METRIC_VIEW_STATEWRT_FIELD_COUNT_TOTAL_WIDTH, 80);
      EclipseUtility.setDefaultIntPreference(METRIC_VIEW_STATEWRT_IMMUTABLE_FIELD_COUNT_WIDTH, 80);
      EclipseUtility.setDefaultIntPreference(METRIC_VIEW_STATEWRT_THREADSAFE_FIELD_COUNT_WIDTH, 80);
      EclipseUtility.setDefaultIntPreference(METRIC_VIEW_STATEWRT_NOTTHREADSAFE_FIELD_COUNT_WIDTH, 80);
      EclipseUtility.setDefaultIntPreference(METRIC_VIEW_STATEWRT_LOCK_PROTECTED_FIELD_COUNT_WIDTH, 80);
      EclipseUtility.setDefaultIntPreference(METRIC_VIEW_STATEWRT_THREADCONFINED_FIELD_COUNT_WIDTH, 80);
      EclipseUtility.setDefaultIntPreference(METRIC_VIEW_STATEWRT_OTHER_FIELD_COUNT_WIDTH, 80);
      EclipseUtility.setDefaultIntPreference(METRIC_VIEW_STATEWRT_SASH_LHS_WEIGHT, 60);
      EclipseUtility.setDefaultIntPreference(METRIC_VIEW_STATEWRT_SASH_RHS_WEIGHT, 40);

      /*
       * We'll take the default-default for the other preferences.
       */
    }
  }

  public static final String SHOW_BALLOON_NOTIFICATIONS = PREFIX + "show-balloon-notifications";
  public static final String ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_SCAN = PREFIX + "always.allow.user.to.select.projects.to.scan";
  public static final String ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_UPDATE_JAR = PREFIX
      + "always.allow.user.to.select.projects.to.update.jar";
  public static final String LAST_TIME_PROJECTS_TO_SCAN = PREFIX + "last.time.projects.to.scan";
  public static final String LAST_TIME_PROJECTS_TO_UPDATE_JAR = PREFIX + "last.time.projects.to.update.jar";

  public static final String REGION_MODEL_NAME_SUFFIX = PREFIX + "regionModelNameSuffix";
  public static final String REGION_MODEL_NAME_CAP = PREFIX + "regionModelNameCap";
  public static final String REGION_MODEL_NAME_COMMON_STRING = PREFIX + "regionModelNameCommonString";
  public static final String LOCK_MODEL_NAME_SUFFIX = PREFIX + "lockModelNameSuffix";
  public static final String LOCK_MODEL_NAME_CAP = PREFIX + "lockModelNameCap";
  public static final String SAVE_DIRTY_EDITORS_BEFORE_VERIFY = PREFIX + "save.before.verify";

  public static final String CURRENT_SCAN = PREFIX + "current.scan";

  public static final String PROPOSED_ANNOTATIONS_SHOW_ABDUCTIVE_ONLY = PREFIX + "proposed.annotations.show.abductive.only";
  public static final String PROPOSED_ANNO_COL_TREE_WIDTH = PREFIX + "proposed.anno.col.tree.width";
  public static final String PROPOSED_ANNO_COL_LINE_WIDTH = PREFIX + "proposed.anno.col.line.width";
  public static final String PROPOSED_ANNO_HIGHLIGHT_DIFFERENCES = PREFIX + "proposed.anno.highlight.differences";
  public static final String PROPOSED_ANNO_SHOW_ONLY_DIFFERENCES = PREFIX + "proposed.anno.only.differences";
  public static final String PROPOSED_ANNO_SHOW_ONLY_FROM_SRC = PREFIX + "proposed.anno.show.only.from.src";

  public static final String VSTATUS_ALPHA_SORT = PREFIX + "verification.status.alpha.sort";
  public static final String VSTATUS_SHOW_HINTS = PREFIX + "verification.status.show.hints";
  public static final String VSTATUS_SHOW_ONLY_DIFFERENCES = PREFIX + "verification.status.only.differences";
  public static final String VSTATUS_HIGHLIGHT_DIFFERENCES = PREFIX + "verification.status.highlight.differences";
  public static final String VSTATUS_TREE_WIDTH = PREFIX + "verification.status.tree.width";
  public static final String VSTATUS_PROJECT_WIDTH = PREFIX + "verification.status.project.width";
  public static final String VSTATUS_PACKAGE_WIDTH = PREFIX + "verification.status.package.width";
  public static final String VSTATUS_TYPE_WIDTH = PREFIX + "verification.status.type.width";
  public static final String VSTATUS_LINE_WIDTH = PREFIX + "verification.status.line.width";
  public static final String VSTATUS_COL_DIFF_WIDTH = PREFIX + "verification.status.col.diff.width";

  public static final String VEXPLORER_HIGHLIGHT_DIFFERENCES = PREFIX + "verification.explorer.highlight.differences";
  public static final String VEXPLORER_SHOW_OBSOLETE_DROP_DIFFERENCES = PREFIX + "verification.explorer.obsolete.drops.differences";
  public static final String VEXPLORER_SHOW_ONLY_DIFFERENCES = PREFIX + "verification.explorer.only.differences";
  public static final String VEXPLORER_SHOW_ONLY_DERIVED_FROM_SRC = PREFIX + "verification.explorer.show.only.derived.from.src";
  public static final String VEXPLORER_SHOW_ANALYSIS_RESULTS = PREFIX + "verification.explorer.show.analysis.results";
  public static final String VEXPLORER_SHOW_HINTS = PREFIX + "verification.explorer.show.hints";
  public static final String VEXPLORER_COL_TREE_WIDTH = PREFIX + "verification.explorer.col.tree.width";
  public static final String VEXPLORER_COL_POSITION_WIDTH = PREFIX + "verification.explorer.col.position.width";
  public static final String VEXPLORER_COL_LINE_WIDTH = PREFIX + "verification.explorer.col.line.width";
  public static final String VEXPLORER_COL_DIFF_WIDTH = PREFIX + "verification.explorer.col.diff.width";

  public static final String PROBLEMS_HIGHLIGHT_DIFFERENCES = PREFIX + "problems.highlight.differences";
  public static final String PROBLEMS_SHOW_ONLY_DIFFERENCES = PREFIX + "problems.only.differences";
  public static final String PROBLEMS_SHOW_ONLY_FROM_SRC = PREFIX + "problems.show.only.from.src";

  public static final String VIEWS_SAVE_TREE_STATE = PREFIX + "views.save.tree.state";

  public static final String METRIC_VIEW_TAB_SELECTION = PREFIX + "metric.view.tab.selection";
  public static final String METRIC_VIEW_SLOC_THRESHOLD = PREFIX + "metric.view.sloc.threshold";
  public static final String METRIC_ALPHA_SORT = PREFIX + "metric.alpha.sort";
  public static final String METRIC_FILTER = PREFIX + "metric.filter";

  public static final String METRIC_SLOC_SASH_LHS_WEIGHT = PREFIX + "metric.sloc.sash.lhs.weight";
  public static final String METRIC_SLOC_SASH_RHS_WEIGHT = PREFIX + "metric.sloc.sash.rhs.weight";
  public static final String METRIC_SLOC_COL_TREE_WIDTH = PREFIX + "metric.sloc.col.tree.width";
  public static final String METRIC_SLOC_COL_BLANK_LINE_COUNT_WIDTH = PREFIX + "metric.sloc.col.blank-line-count.width";
  public static final String METRIC_SLOC_COL_CONTAINS_COMMENT_LINE_COUNT_WIDTH = PREFIX
      + "metric.sloc.col.contains-comment-line-count.width";
  public static final String METRIC_SLOC_COL_JAVA_DECLARATION_COUNT_WIDTH = PREFIX + "metric.sloc.col.java-declaration-count.width";
  public static final String METRIC_SLOC_COL_JAVA_STATEMENT_COUNT_WIDTH = PREFIX + "metric.sloc.col.java-statement-count.width";
  public static final String METRIC_SLOC_COL_LINE_COUNT_WIDTH = PREFIX + "metric.sloc.col.line-count.width";
  public static final String METRIC_SLOC_COL_SEMICOLON_COUNT_WIDTH = PREFIX + "metric.sloc.col.semicolon-count.width";
  public static final String METRIC_VIEW_SLOC_COMBO_SELECTED_COLUMN = PREFIX + "metric.sloc.combo_seleted_column";
  public static final String METRIC_VIEW_SLOC_THRESHOLD_SHOW_ABOVE = PREFIX + "metric.sloc.threshold_show_above";

  public static final String METRIC_SCAN_TIME_COL_TREE_WIDTH = PREFIX + "metric.scan.time.col.tree.width";
  public static final String METRIC_SCAN_TIME_COL_DURATION_WIDTH = PREFIX + "metric.scan.time.col.duration.width";
  public static final String METRIC_SCAN_TIME_ANALYSIS_TO_SHOW = PREFIX + "metric.scan.time.analysis.to-show";
  public static final String METRIC_SCAN_TIME_THRESHOLD_MS = PREFIX + "metric.scan.time.threshold-ms";
  public static final String METRIC_SCAN_TIME_THRESHOLD_SHOW_ABOVE = PREFIX + "metric.scan.time.threshold_show_above";

  public static final String METRIC_DROP_COUNTER_COL_DROP_WIDTH = PREFIX + "metric.drop.counter.col.drop.width";
  public static final String METRIC_DROP_COUNTER_COL_COUNT_WIDTH = PREFIX + "metric.drop.counter.col.count.width";

  public static final String METRIC_VIEW_STATEWRT_COMBO_SELECTED_COLUMN = PREFIX + "metric.statewrt.combo_seleted_column";
  public static final String METRIC_VIEW_STATEWRT_THRESHOLD_SHOW_ABOVE = PREFIX + "metric.statewrt.threshold_show_above";
  public static final String METRIC_VIEW_STATEWRT_THRESHOLD = PREFIX + "metric.view.statewrt.threshold";
  public static final String METRIC_VIEW_STATEWRT_TREE_WIDTH = PREFIX + "metric.view.statewrt.tree.width";
  public static final String METRIC_VIEW_STATEWRT_FIELD_COUNT_TOTAL_WIDTH = PREFIX + "metric.view.statewrt.field-count-total.width";
  public static final String METRIC_VIEW_STATEWRT_IMMUTABLE_FIELD_COUNT_WIDTH = PREFIX
      + "metric.view.statewrt.immutable-field-count.width";
  public static final String METRIC_VIEW_STATEWRT_THREADSAFE_FIELD_COUNT_WIDTH = PREFIX
      + "metric.view.statewrt.threadsafe-field-count.width";
  public static final String METRIC_VIEW_STATEWRT_NOTTHREADSAFE_FIELD_COUNT_WIDTH = PREFIX
      + "metric.view.statewrt.notthreadsafe-field-count.width";
  public static final String METRIC_VIEW_STATEWRT_LOCK_PROTECTED_FIELD_COUNT_WIDTH = PREFIX
      + "metric.view.statewrt.lock-protected-field-count.width";
  public static final String METRIC_VIEW_STATEWRT_THREADCONFINED_FIELD_COUNT_WIDTH = PREFIX
      + "metric.view.statewrt.threadconfined-field-count.width";
  public static final String METRIC_VIEW_STATEWRT_OTHER_FIELD_COUNT_WIDTH = PREFIX + "metric.view.statewrt.other-field-count.width";
  public static final String METRIC_VIEW_STATEWRT_SASH_LHS_WEIGHT = PREFIX + "metric.view.statewrt.sash.lhs.weight";
  public static final String METRIC_VIEW_STATEWRT_SASH_RHS_WEIGHT = PREFIX + "metric.view.statewrt.sash.rhs.weight";

  /**
   * Gets the JSure data directory. This method ensures that the directory does
   * exist on the disk. It checks that is is there and, if not, tries to create
   * it. If it can't be created the method throws an exception.
   * 
   * @return the JSure data directory.
   * 
   * @throws Exception
   *           if the JSure data directory doesn't exist on the disk and can't
   *           be created.
   */
  @NonNull
  public static File getJSureDataDirectory() {
    final File result = EclipseUtility.getJSureDataDirectory();
    FileUtility.ensureDirectoryExists(result);
    return result;
  }

  /**
   * Gets the JSure library promises XML directory where local changes to the
   * XML are stored. This method ensures that the directory does exist on the
   * disk. It checks that is is there and, if not, tries to create it. If it
   * can't be created the method throws an exception.
   * 
   * @return the JSure XML diff directory.
   * 
   * @throws IllegalStateException
   *           if the JSure data directory doesn't exist on the disk and can't
   *           be created.
   */
  public static File getJSureXMLDirectory() {
    final String path = EclipseUtility.getStringPreference(IDEPreferences.JSURE_XML_DIFF_DIRECTORY);
    final File result = new File(path);
    FileUtility.ensureDirectoryExists(path);
    return result;
  }

  /**
   * Gets the switch-to-the-JSure-perspective preferences.
   * 
   * @return the switch-to-the-JSure-perspective preferences.
   */
  public static AutoPerspectiveSwitchPreferences getSwitchPreferences() {
    return new AutoPerspectiveSwitchPreferences() {
      @Override
      public String getConstant(String suffix) {
        return PREFIX + suffix;
      }
    };
  }

  /**
   * Gets the memory size to be used by the remote JSure process
   */
  public static int getMaxMemorySize() {
    return EclipseUtility.getIntPreference(IDEPreferences.TOOL_MEMORY_MB);
  }

  private JSurePreferencesUtility() {
    // Utility
  }
}
