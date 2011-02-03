package com.surelogic.jsure.client.eclipse.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.common.ui.preferences.IAutoPerspectiveSwitchPreferences;
import com.surelogic.jsure.client.eclipse.Activator;

public class PreferenceConstants implements IAutoPerspectiveSwitchPreferences {
	private static final String PREFIX = "com.surelogic.jsure.";

	public static final String P_PROMPT_PERSPECTIVE_SWITCH = PREFIX
			+ PROMPT_PERSPECTIVE_SWITCH;

	public boolean getPromptForPerspectiveSwitch() {
		return Activator.getDefault().getPreferenceStore().getBoolean(
				P_PROMPT_PERSPECTIVE_SWITCH);
	}

	public void setPromptForPerspectiveSwitch(boolean value) {
		Activator.getDefault().getPreferenceStore().setValue(
				P_PROMPT_PERSPECTIVE_SWITCH, value);
	}

	public static final String P_AUTO_PERSPECTIVE_SWITCH = PREFIX
			+ AUTO_PERSPECTIVE_SWITCH;

	public boolean getAutoPerspectiveSwitch() {
		return Activator.getDefault().getPreferenceStore().getBoolean(
				P_AUTO_PERSPECTIVE_SWITCH);
	}

	public void setAutoPerspectiveSwitch(boolean value) {
		Activator.getDefault().getPreferenceStore().setValue(
				P_AUTO_PERSPECTIVE_SWITCH, value);
	}

	public static final String P_AUTO_OPEN_MODELING_PROBLEMS_VIEW = PREFIX
			+ "open.modeling.problems.view";

	public boolean getAutoOpenModelingProblemsView() {
		return Activator.getDefault().getPreferenceStore().getBoolean(
				P_AUTO_OPEN_MODELING_PROBLEMS_VIEW);
	}

	public void setAutoOpenModelingProblemsView(boolean value) {
		Activator.getDefault().getPreferenceStore().setValue(
				P_AUTO_OPEN_MODELING_PROBLEMS_VIEW, value);
	}

	public static final String P_AUTO_OPEN_PROPOSED_PROMISE_VIEW = PREFIX
			+ "open.proposed.promise.view";

	public boolean getAutoOpenProposedPromiseView() {
		return Activator.getDefault().getPreferenceStore().getBoolean(
				P_AUTO_OPEN_MODELING_PROBLEMS_VIEW);
	}

	public void setAutoOpenProposedPromiseView(boolean value) {
		Activator.getDefault().getPreferenceStore().setValue(
				P_AUTO_OPEN_MODELING_PROBLEMS_VIEW, value);
	}

	/**
	 * Whether we should build only when the user says so, or when Eclipse says
	 * to do so
	 */
	public static final String P_AUTO_ANALYZE_ON_BUILD = PREFIX + "auto.analyze.on.build";

	public static boolean getAutoAnalyzeOnBuild() {
		return Activator.getDefault().getPreferenceStore().getBoolean(
				P_AUTO_ANALYZE_ON_BUILD);
	}

	public static void setAutoAnalyzeOnBuild(boolean value) {
		Activator.getDefault().getPreferenceStore().setValue(
				P_AUTO_ANALYZE_ON_BUILD, value);
	}
	
	private PreferenceConstants() {
		// Nothing to do
	}

	public static final PreferenceConstants prototype = new PreferenceConstants();

	public String getPrefConstant(String suffix) {
		return PREFIX + suffix;
	}

	public IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}
}
