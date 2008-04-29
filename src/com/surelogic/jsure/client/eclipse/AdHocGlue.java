package com.surelogic.jsure.client.eclipse;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.*;

import com.surelogic.adhoc.AbstractAdHocGlue;

public final class AdHocGlue extends AbstractAdHocGlue {
	private static final ExecutorService exec = 
        Executors.newSingleThreadExecutor();
	
	public Executor getExecutor() {
		return exec;
	}
	
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
