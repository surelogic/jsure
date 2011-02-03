package com.surelogic.fluid.eclipse.preferences;

import java.io.File;

import org.eclipse.jface.preference.IPreferenceStore;

import edu.cmu.cs.fluid.eclipse.ui.FluidPlugin;
import edu.cmu.cs.fluid.ide.IDEPreferences;

public class PreferenceConstants {
	private static final String PREFIX = "com.surelogic.fluid.";

	public static final String P_ALLOW_JAVADOC_ANNOS = PREFIX
			+ IDEPreferences.ALLOW_JAVADOC_ANNOS;

	public static boolean allowJavadocAnnos() {
		return FluidPlugin.getDefault().getPreferenceStore()
				.getBoolean(P_ALLOW_JAVADOC_ANNOS);
	}

	public static void setAllowJavadocAnnos(boolean value) {
		FluidPlugin.getDefault().getPreferenceStore()
				.setValue(P_ALLOW_JAVADOC_ANNOS, value);
	}

	public static final String P_ANALYSIS_THREAD_COUNT = PREFIX
			+ IDEPreferences.ANALYSIS_THREAD_COUNT;

	/**
	 * Gets the number of threads that the analysis is allowed to use. The
	 * result will always be positive.
	 * 
	 * @return the number of threads that the analysis is allowed to use. The
	 *         result will always be positive (i.e., one or greater).
	 */
	public int getAnalysisThreadCount() {
		int result = FluidPlugin.getDefault().getPreferenceStore()
				.getInt(P_ANALYSIS_THREAD_COUNT);
		if (result < 1)
			result = 1;
		return result;
	}

	public static final String P_REGION_MODEL_NAME_SUFFIX = PREFIX
			+ "regionModelNameSuffix";
	public static final String P_REGION_MODEL_NAME_CAP = PREFIX
			+ "regionModelNameCap";
	public static final String P_REGION_MODEL_NAME_COMMON_STRING = PREFIX
			+ "regionModelNameCommonString";
	public static final String P_LOCK_MODEL_NAME_SUFFIX = PREFIX
			+ "lockModelNameSuffix";
	public static final String P_LOCK_MODEL_NAME_CAP = PREFIX
			+ "lockModelNameCap";

	public static final String P_DATA_DIRECTORY = PREFIX
			+ IDEPreferences.DATA_DIRECTORY;

	public static File getJSureDataDirectory() {
		final String path = FluidPlugin.getDefault().getPreferenceStore()
				.getString(P_DATA_DIRECTORY);
		if (path.length() == 0 || path == null) {
			return null;
		}
		return new File(path);
	}

	public static void setJSureDataDirectory(final File dir) {
		if (dir != null && dir.exists() && dir.isDirectory()) {
			FluidPlugin.getDefault().getPreferenceStore()
					.setValue(P_DATA_DIRECTORY, dir.getAbsolutePath());
		} else {
			throw new IllegalArgumentException("Bad directory: " + dir);
		}
	}

	private PreferenceConstants() {
		// Nothing to do
	}

	public static final PreferenceConstants prototype = new PreferenceConstants();

	private static String getPrefConstant(String suffix) {
		return PREFIX + suffix;
	}

	public static IPreferenceStore getPreferenceStore() {
		return FluidPlugin.getDefault().getPreferenceStore();
	}

	public static boolean getBooleanPreference(String suffix) {
		return getPreferenceStore().getBoolean(getPrefConstant(suffix));
	}

	public static int getIntPreference(String suffix) {
		return getPreferenceStore().getInt(getPrefConstant(suffix));
	}

	public static String getStringPreference(String suffix) {
		return getPreferenceStore().getString(getPrefConstant(suffix));
	}
}
