package com.surelogic.jsure.client.eclipse.preferences;

import java.io.File;

import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.common.eclipse.preferences.IPreferenceConstants;
import com.surelogic.jsure.client.eclipse.Activator;

public class PreferenceConstants implements IPreferenceConstants {
  private static final String PREFIX = "com.surelogic.jsure.";

  public static final String P_PROMPT_PERSPECTIVE_SWITCH = PREFIX
      + PROMPT_PERSPECTIVE_SWITCH;

  public boolean getPromptForPerspectiveSwitch() {
    return Activator.getDefault().getPluginPreferences().getBoolean(
        P_PROMPT_PERSPECTIVE_SWITCH);
  }

  public void setPromptForPerspectiveSwitch(boolean value) {
    Activator.getDefault().getPluginPreferences().setValue(
        P_PROMPT_PERSPECTIVE_SWITCH, value);
  }

  public static final String P_AUTO_PERSPECTIVE_SWITCH = PREFIX
      + AUTO_PERSPECTIVE_SWITCH;

  public boolean getAutoPerspectiveSwitch() {
    return Activator.getDefault().getPluginPreferences().getBoolean(
        P_AUTO_PERSPECTIVE_SWITCH);
  }

  public void setAutoPerspectiveSwitch(boolean value) {
    Activator.getDefault().getPluginPreferences().setValue(
        P_AUTO_PERSPECTIVE_SWITCH, value);
  }

  public static final String P_AUTO_OPEN_MODELING_PROBLEMS_VIEW = PREFIX
      + "open.modeling.problems.view";

  public boolean getAutoOpenModelingProblemsView() {
    return Activator.getDefault().getPluginPreferences().getBoolean(
        P_AUTO_OPEN_MODELING_PROBLEMS_VIEW);
  }

  public void setAutoOpenModelingProblemsView(boolean value) {
    Activator.getDefault().getPluginPreferences().setValue(
        P_AUTO_OPEN_MODELING_PROBLEMS_VIEW, value);
  }

  public static final String P_DATA_DIRECTORY = PREFIX + DATA_DIRECTORY;
  
  public static File getJSureDataDirectory() {
	  final String path = Activator.getDefault().getPluginPreferences()
	  .getString(P_DATA_DIRECTORY);
	  if (path.length() == 0 || path == null) {
		  return null;
	  }
	  return new File(path);
  }

  public static void setJSureDataDirectory(final File dir) {
	  if (dir != null && dir.exists() && dir.isDirectory()) {
		  Activator.getDefault().getPluginPreferences().setValue(
				  P_DATA_DIRECTORY, dir.getAbsolutePath());
	  } else {
		  throw new IllegalArgumentException("Bad directory: " + dir);
	  }
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
