package com.surelogic.jsure.client.eclipse.preferences;

import com.surelogic.jsure.client.eclipse.Activator;

public class PreferenceConstants {

	public static final String P_SIERRA_DELETE_DB_ON_STARTUP = "com.surelogic.sierra.delete-db-on-startup";

	public static boolean deleteDatabaseOnStartup() {
		return Activator.getDefault().getPluginPreferences().getBoolean(
				P_SIERRA_DELETE_DB_ON_STARTUP);
	}

	public static void setDeleteDatabaseOnStartup(
			boolean deleteDatabaseOnStartup) {
		Activator.getDefault().getPluginPreferences().setValue(
				P_SIERRA_DELETE_DB_ON_STARTUP, deleteDatabaseOnStartup);
	}
}
