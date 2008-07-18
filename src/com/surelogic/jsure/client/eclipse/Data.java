package com.surelogic.jsure.client.eclipse;

import com.surelogic.common.derby.DerbyConnection;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.jsure.schema.JSureSchemaData;

public final class Data extends DerbyConnection {

	private Data() {
		// no instances
	}

	private static final String SCHEMA_NAME = "JSURE";
	private static final String DATABASE_DIR = "db";

	@Override
	protected boolean deleteDatabaseOnStartup() {
		return false;
	}

	@Override
	protected String getDatabaseLocation() {
		return Activator.getDefault().getLocation(DATABASE_DIR);
	}

	@Override
	protected SchemaData getSchemaLoader() {
		return new JSureSchemaData();
	}

	@Override
	protected String getSchemaName() {
		return SCHEMA_NAME;
	}

	@Override
	protected void setDeleteDatabaseOnStartup(final boolean bool) {
		// Do nothing
	}

	private static final Data INSTANCE = new Data();

	public static Data getInstance() {
		return INSTANCE;
	}

}
