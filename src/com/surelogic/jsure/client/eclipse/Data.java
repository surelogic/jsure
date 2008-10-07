package com.surelogic.jsure.client.eclipse;

import com.surelogic.common.derby.DerbyConnection;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.jsure.schema.JSureSchemaData;

public final class Data extends DerbyConnection {

	private static final Data INSTANCE = new Data();

	public static Data getInstance() {
		INSTANCE.loggedBootAndCheckSchema();
		return INSTANCE;
	}

	private Data() {
		// singleton
	}

	@Override
	protected String getDatabaseLocation() {
		return Activator.getDefault().getLocation(DATABASE_PATH_FRAGMENT);
	}

	@Override
	protected String getSchemaName() {
		return "JSURE";
	}

	public SchemaData getSchemaLoader() {
		return new JSureSchemaData();
	}
}
