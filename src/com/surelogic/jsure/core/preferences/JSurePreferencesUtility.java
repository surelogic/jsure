package com.surelogic.jsure.core.preferences;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import com.surelogic.common.FileUtility;
import com.surelogic.common.core.EclipseUtility;
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

			EclipseUtility.setDefaultBooleanPreference(
					AUTO_OPEN_MODELING_PROBLEMS_VIEW, true);
			EclipseUtility.setDefaultBooleanPreference(
					AUTO_OPEN_PROPOSED_PROMISE_VIEW, true);
			EclipseUtility.setDefaultBooleanPreference(getSwitchPreferences()
					.getPromptPerspectiveSwitchConstant(), true);
			EclipseUtility.setDefaultBooleanPreference(getSwitchPreferences()
					.getAutoPerspectiveSwitchConstant(), true);

			EclipseUtility.setDefaultBooleanPreference(AUTO_ANALYZE_ON_BUILD,
					true);

			EclipseUtility
					.setDefaultStringPreference(
							IDEPreferences.JSURE_DATA_DIRECTORY,
							EclipseUtility
									.getADataDirectoryPath(FileUtility.JSURE_DATA_PATH_FRAGMENT));

			/*
			 * We'll take the default-default for the other preferences.
			 */
		}
	}

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

	public static final String AUTO_OPEN_MODELING_PROBLEMS_VIEW = PREFIX
			+ "open.modeling.problems.view";
	public static final String AUTO_OPEN_PROPOSED_PROMISE_VIEW = PREFIX
			+ "open.proposed.promise.view";

	/**
	 * Whether we should build only when the user says so (false), or when
	 * Eclipse says to do so (true).
	 */
	public static final String AUTO_ANALYZE_ON_BUILD = PREFIX
			+ "auto.analyze.on.build";

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
	 * Sets the JSure data directory to an existing directory.
	 * <p>
	 * This method simply changes the preference it does not move data from the
	 * old directory (or even delete the old directory).
	 * 
	 * @param dir
	 *            the new JSure data directory (must exist and be a directory).
	 * 
	 * @throws IllegalArgumentException
	 *             if the passed {@link File} is not a directory or doesn't
	 *             exist.
	 */
	public static void setJSureDataDirectory(final File dir) {
		if (dir != null && dir.isDirectory()) {
			EclipseUtility.setStringPreference(
					IDEPreferences.JSURE_DATA_DIRECTORY, dir.getAbsolutePath());
		} else {
			throw new IllegalArgumentException("Bad JSure data directory "
					+ dir + " it doesn't exist on the disk");
		}
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

	private JSurePreferencesUtility() {
		// Utility
	}
}
