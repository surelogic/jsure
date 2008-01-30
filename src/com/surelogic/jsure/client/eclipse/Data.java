package com.surelogic.jsure.client.eclipse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.surelogic.common.eclipse.jdbc.DerbyDataUtils;
import com.surelogic.common.jdbc.LazyPreparedStatementConnection;
import com.surelogic.jsure.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.jsure.schema.JSureSchemaUtility;

public final class Data {

	private Data() {
		// no instances
	}

	private static final String SCHEMA_NAME = "JSURE";
	private static final String DATABASE_DIR = "db";

	public static void bootAndCheckSchema() throws Exception {
		if (PreferenceConstants.deleteDatabaseOnStartup()) {
			/*
			 * Delete the database
			 */
			try {
			  DerbyDataUtils.deleteDatabase(getDatabaseLocation());
			} finally {
				PreferenceConstants.setDeleteDatabaseOnStartup(false);
			}
		}

		DerbyDataUtils.createDatabase(JSureSchemaUtility.getDefault(), getDatabaseLocation(), SCHEMA_NAME);
	}

	public static Connection readOnlyConnection() throws SQLException {
		Connection conn = getConnection();
		conn.setReadOnly(true);
		return conn;
	}

	public static Connection transactionConnection() throws SQLException {
		Connection conn = getConnection();
		conn.setAutoCommit(false);
		return conn;
	}

	public static Connection getConnection() throws SQLException {
		Connection conn = LazyPreparedStatementConnection.wrap(DriverManager
				.getConnection(getConnectionURL()));
		return conn;
	}

	private static String getConnectionURL() {
		return DerbyDataUtils.getConnectionURL(getDatabaseLocation(), SCHEMA_NAME);
	}

	private static String getDatabaseLocation() {
		return Activator.getDefault().getLocation(DATABASE_DIR);
	}
}
