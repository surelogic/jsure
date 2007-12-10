package com.surelogic.jsure.client.eclipse;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import com.surelogic.adhoc.IAdHoc;

public final class AdHocGlue implements IAdHoc {
	public Connection getConnection() throws SQLException {
		return Data.getConnection();
	}

	public int getMaxRowsPerQuery() {
		return 5000;
	}

	public File getQuerySaveFile() {
		return new File(Activator.getDefault().getLocation("queries.xml"));
	}
}
