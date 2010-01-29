package com.surelogic.jsure.client.eclipse.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.common.FileUtility;
import com.surelogic.common.eclipse.preferences.AbstractPrefInitializer;
import com.surelogic.jsure.client.eclipse.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPrefInitializer {

  @Override
  public void initializeDefaultPreferences() {
    final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
    store.setDefault(PreferenceConstants.P_PROMPT_PERSPECTIVE_SWITCH, true);
    store.setDefault(PreferenceConstants.P_AUTO_PERSPECTIVE_SWITCH, false);
    store.setDefault(PreferenceConstants.P_AUTO_OPEN_MODELING_PROBLEMS_VIEW,
        true);
	store.setDefault(PreferenceConstants.P_DATA_DIRECTORY,
			getDefaultDataDirectory(FileUtility.JSURE_DATA_PATH_FRAGMENT));
	
	// Get the data directory and ensure that it actually exists.
	final String path = store
			.getString(PreferenceConstants.P_DATA_DIRECTORY);
	ensureDataDirectoryExists(path);
  }
}
