package com.surelogic.jsure.core.preferences;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import com.surelogic.analysis.IAnalysisInfo;
import com.surelogic.common.FileUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.MemoryUtility;
import com.surelogic.common.core.preferences.AutoPerspectiveSwitchPreferences;
import com.surelogic.javac.Javac;

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

	private static final AtomicBoolean f_initializationNeeded = new AtomicBoolean(
			true);

	/**
	 * Sets up the default values for the JSure tool.
	 * <p>
	 * <b>WARNING:</b> Because this class exports strings that are declared to
	 * be {@code public static final} simply referencing these constants may not
	 * trigger Eclipse to load the containing plug-in. This is because the
	 * constants are copied by the Java compiler into using class files. This
	 * means that each using plug-in <b>must</b> invoke
	 * {@link #initializeDefaultScope()} in its plug-in activator's
	 * {@code start} method.
	 */
	public static void initializeDefaultScope() {
		if (f_initializationNeeded.compareAndSet(true, false)) {

			int cpuCount = Runtime.getRuntime().availableProcessors();
			if (cpuCount < 1)
				cpuCount = 1;
			EclipseUtility.setDefaultIntPreference(
					IDEPreferences.ANALYSIS_THREAD_COUNT, cpuCount);

			// Underestimate a little bit
			final int estimatedMax = MemoryUtility.computeMaxMemorySizeInMb() - 256;
			int mem = 2048;
			while (mem > estimatedMax) {
				mem -= 512;
			}
			EclipseUtility.setDefaultIntPreference(
					IDEPreferences.TOOL_MEMORY_MB, mem);
			EclipseUtility.setDefaultBooleanPreference(
					IDEPreferences.LOAD_ALL_CLASSES, false);

			EclipseUtility.setDefaultStringListPreference(
					IDEPreferences.MODELING_PROBLEM_FILTERS,
					ModelingProblemFilterUtility.DEFAULT);

			EclipseUtility.setDefaultBooleanPreference(
					SHOW_BALLOON_NOTIFICATIONS, true);
			EclipseUtility.setDefaultBooleanPreference(getSwitchPreferences()
					.getPromptPerspectiveSwitchConstant(), true);
			EclipseUtility.setDefaultBooleanPreference(getSwitchPreferences()
					.getAutoPerspectiveSwitchConstant(), true);
			EclipseUtility.setDefaultBooleanPreference(
					ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_SCAN, true);
			EclipseUtility.setDefaultBooleanPreference(
					ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_UPDATE_JAR, true);

			EclipseUtility.setDefaultStringPreference(REGION_MODEL_NAME_SUFFIX,
					"State");
			EclipseUtility.setDefaultBooleanPreference(REGION_MODEL_NAME_CAP,
					true);
			EclipseUtility.setDefaultBooleanPreference(
					REGION_MODEL_NAME_COMMON_STRING, true);
			EclipseUtility.setDefaultStringPreference(LOCK_MODEL_NAME_SUFFIX,
					"Lock");
			EclipseUtility.setDefaultBooleanPreference(LOCK_MODEL_NAME_CAP,
					true);

			String dataDir = EclipseUtility
					.getADataDirectoryPath(FileUtility.JSURE_DATA_PATH_FRAGMENT);
			EclipseUtility.setDefaultStringPreference(
					IDEPreferences.JSURE_DATA_DIRECTORY, dataDir);

			File xmlDiffDir = new File(dataDir,
					FileUtility.JSURE_XML_DIFF_PATH_FRAGMENT);
			EclipseUtility.setDefaultStringPreference(
					IDEPreferences.JSURE_XML_DIFF_DIRECTORY,
					xmlDiffDir.getAbsolutePath());

			for (IAnalysisInfo a : Javac.getDefault().getAnalysisInfo()) {
				// System.out.println("Defaulting "+a.getUniqueIdentifier()+" to "+a.isProduction());
				EclipseUtility.setDefaultBooleanPreference(
						IDEPreferences.ANALYSIS_ACTIVE_PREFIX
								+ a.getUniqueIdentifier(), a.isProduction());
			}

			EclipseUtility.setDefaultIntPreference(
					IDEPreferences.TIMEOUT_WARNING_SEC, 30);
			EclipseUtility.setDefaultBooleanPreference(
					IDEPreferences.TIMEOUT_FLAG, true);
			EclipseUtility.setDefaultIntPreference(IDEPreferences.TIMEOUT_SEC,
					60);

			EclipseUtility.setDefaultBooleanPreference(
					SAVE_DIRTY_EDITORS_BEFORE_VERIFY, false);

			EclipseUtility.setDefaultBooleanPreference(
					PROPOSED_PROMISES_AS_TREE, true);
			EclipseUtility.setDefaultBooleanPreference(
					PROPOSED_PROMISES_SHOW_ABDUCTIVE_ONLY, true);

			/*
			 * We'll take the default-default for the other preferences.
			 */
		}
	}

	public static final String SHOW_BALLOON_NOTIFICATIONS = PREFIX
			+ "show-balloon-notifications";
	public static final String ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_SCAN = PREFIX
			+ "always.allow.user.to.select.projects.to.scan";
	public static final String ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_UPDATE_JAR = PREFIX
			+ "always.allow.user.to.select.projects.to.update.jar";
	public static final String LAST_TIME_PROJECTS_TO_SCAN = PREFIX
			+ "last.time.projects.to.scan";
	public static final String LAST_TIME_PROJECTS_TO_UPDATE_JAR = PREFIX
			+ "last.time.projects.to.update.jar";

	public static final String REGION_MODEL_NAME_SUFFIX = PREFIX
			+ "regionModelNameSuffix";
	public static final String REGION_MODEL_NAME_CAP = PREFIX
			+ "regionModelNameCap";
	public static final String REGION_MODEL_NAME_COMMON_STRING = PREFIX
			+ "regionModelNameCommonString";
	public static final String LOCK_MODEL_NAME_SUFFIX = PREFIX
			+ "lockModelNameSuffix";
	public static final String LOCK_MODEL_NAME_CAP = PREFIX
			+ "lockModelNameCap";
	public static final String SAVE_DIRTY_EDITORS_BEFORE_VERIFY = PREFIX
			+ "save.before.verify";

	public static final String CURRENT_SCAN = PREFIX + "current.scan";

	public static final String PROPOSED_PROMISES_AS_TREE = PREFIX
			+ "proposed.promises.as.tree";
	public static final String PROPOSED_PROMISES_SHOW_ABDUCTIVE_ONLY = PREFIX
			+ "proposed.promises.show.abductive.only";

	/**
	 * Gets the JSure data directory. This method ensures that the directory
	 * does exist on the disk. It checks that is is there and, if not, tries to
	 * create it. If it can't be created the method throws an exception.
	 * 
	 * @return the JSure data directory.
	 * 
	 * @throws IllegalStateException
	 *             if the JSure data directory doesn't exist on the disk and
	 *             can't be created.
	 */
	public static File getJSureDataDirectory() {
		final String path = EclipseUtility
				.getStringPreference(IDEPreferences.JSURE_DATA_DIRECTORY);
		final File result = new File(path);
		FileUtility.ensureDirectoryExists(path);
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
	 *             if the JSure data directory doesn't exist on the disk and
	 *             can't be created.
	 */
	public static File getJSureXMLDirectory() {
		final String path = EclipseUtility
				.getStringPreference(IDEPreferences.JSURE_XML_DIFF_DIRECTORY);
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
