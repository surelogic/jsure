package com.surelogic.fluid.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.common.FileUtility;
import com.surelogic.common.core.EclipseUtility;

import edu.cmu.cs.fluid.eclipse.ui.FluidPlugin;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = FluidPlugin.getDefault()
				.getPreferenceStore();
		store.setDefault(PreferenceConstants.P_ALLOW_JAVADOC_ANNOS, false);
		int availableProcessors = Runtime.getRuntime().availableProcessors();
		if (availableProcessors < 1)
			availableProcessors = 1;
		store.setDefault(PreferenceConstants.P_ANALYSIS_THREAD_COUNT,
				availableProcessors);

		store.setDefault(PreferenceConstants.P_REGION_MODEL_NAME_SUFFIX,
				"State");
		store.setDefault(PreferenceConstants.P_REGION_MODEL_NAME_CAP, true);
		store.setDefault(PreferenceConstants.P_REGION_MODEL_NAME_COMMON_STRING,
				true);

		store.setDefault(PreferenceConstants.P_LOCK_MODEL_NAME_SUFFIX, "Lock");
		store.setDefault(PreferenceConstants.P_LOCK_MODEL_NAME_CAP, true);

		store.setDefault(PreferenceConstants.P_DATA_DIRECTORY, EclipseUtility
				.getADataDirectoryPath(FileUtility.JSURE_DATA_PATH_FRAGMENT));

		// Get the data directory and ensure that it actually exists.
		final String path = store
				.getString(PreferenceConstants.P_DATA_DIRECTORY);
		FileUtility.ensureDirectoryExists(path);
	}
}
