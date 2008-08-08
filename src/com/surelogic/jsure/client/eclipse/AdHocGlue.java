package com.surelogic.jsure.client.eclipse;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

import com.surelogic.common.adhoc.IAdHocDataSource;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

public final class AdHocGlue implements IAdHocDataSource {

	public Connection getConnection() throws SQLException {
		return Data.getInstance().getConnection();
	}

	public int getMaxRowsPerQuery() {
		return 5000;
	}

	public File getQuerySaveFile() {
		return new File(Activator.getDefault().getLocation("queries.xml"));
	}

	public void badQuerySaveFileNotification(Exception e) {
		SLLogger.getLogger().log(Level.SEVERE,
				I18N.err(4, getQuerySaveFile().getAbsolutePath()), e);
	}
}
