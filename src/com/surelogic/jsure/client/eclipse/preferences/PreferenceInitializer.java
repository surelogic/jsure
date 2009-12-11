package com.surelogic.jsure.client.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.jsure.client.eclipse.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences() {
    final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
    store.setDefault(PreferenceConstants.P_PROMPT_PERSPECTIVE_SWITCH, true);
    store.setDefault(PreferenceConstants.P_AUTO_PERSPECTIVE_SWITCH, false);
    store.setDefault(PreferenceConstants.P_AUTO_OPEN_MODELING_PROBLEMS_VIEW,
        true);
  }
}
