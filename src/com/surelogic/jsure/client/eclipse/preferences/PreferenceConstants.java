package com.surelogic.jsure.client.eclipse.preferences;

import com.surelogic.common.eclipse.preferences.IPreferenceConstants;
import com.surelogic.jsure.client.eclipse.Activator;

public class PreferenceConstants implements IPreferenceConstants {
	private static final String PREFIX = "com.surelogic.jsure.";

	public static final String P_PROMPT_PERSPECTIVE_SWITCH = PREFIX + "perspective-switch-prompt";

	public boolean getPromptForPerspectiveSwitch() {
		return Activator.getDefault().getPluginPreferences().getBoolean(
				P_PROMPT_PERSPECTIVE_SWITCH);
	}

	public void setPromptForPerspectiveSwitch(boolean value) {
		Activator.getDefault().getPluginPreferences().setValue(
				P_PROMPT_PERSPECTIVE_SWITCH, value);
	}
	
	public static final String P_AUTO_PERSPECTIVE_SWITCH = PREFIX + "perspective.switch.auto";

	public boolean getAutoPerspectiveSwitch() {
		return Activator.getDefault().getPluginPreferences().getBoolean(
				P_AUTO_PERSPECTIVE_SWITCH);
	}

	public void setAutoPerspectiveSwitch(boolean value) {
		Activator.getDefault().getPluginPreferences().setValue(
				P_AUTO_PERSPECTIVE_SWITCH, value);
	}
	
	private PreferenceConstants() {
		// Nothing to do
	}
	
	public static final PreferenceConstants prototype = new PreferenceConstants();
}
