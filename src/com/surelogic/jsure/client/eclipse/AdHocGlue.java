package com.surelogic.jsure.client.eclipse;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.surelogic.adhoc.IAdHoc;

public final class AdHocGlue implements IAdHoc {

	private static final ExecutorService exec = Executors
			.newSingleThreadExecutor();

	public Executor getExecutor() {
		return exec;
	}

	public Connection getConnection() throws SQLException {
		return Data.getInstance().getConnection();
	}

	public int getMaxRowsPerQuery() {
		return 5000;
	}

	public File getQuerySaveFile() {
		return new File(Activator.getDefault().getLocation("queries.xml"));
	}
}
