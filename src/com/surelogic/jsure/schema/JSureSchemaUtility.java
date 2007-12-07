package com.surelogic.jsure.schema;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

import com.surelogic.common.jdbc.AbstractSchemaUtility;
import com.surelogic.common.jdbc.IDBType;
import com.surelogic.common.jdbc.ISchemaUtility;
import com.surelogic.common.jdbc.SchemaAction;
import com.surelogic.common.jdbc.SchemaUtility;
import com.surelogic.jsure.jdbc.DBType;
import com.surelogic.jsure.jdbc.JSureJDBCUtils;

public final class JSureSchemaUtility extends AbstractSchemaUtility {
	private JSureSchemaUtility() {
		// no other instances
	}

	private static ISchemaUtility prototype = new JSureSchemaUtility();
	
  public static ISchemaUtility getDefault() {
    return prototype;
  }
	
	/**
	 * Up this number when you add a new schema version SQL script to this
	 * package.
	 */
	public static final int schemaVersion = 0;

	public static final String PREFIX = "com.surelogic.jsure.schema.";
	public static final String SQL_SCRIPT_PREFIX = "/"+PREFIX.replace('.', '/');

	public static final String SERVER_PREFIX = "server";
	public static final String ACTION_PREFIX = PREFIX;
	public static final String ACTION_COMMON = ACTION_PREFIX + "Schema" + SEPARATOR;
	public static final String ACTION_SERVER = ACTION_PREFIX + "Server" + SEPARATOR;

  @Override
  protected IDBType getDBType(Connection c) throws SQLException {
    return JSureJDBCUtils.getDb(c);
  }

  @Override
  protected int getSchemaVersion() {
    return schemaVersion;
  }
  
  @Override
  protected SchemaAction getSchemaAction(String prefix, String num, boolean isServer) {
    if (isServer) {
      throw new UnsupportedOperationException("No server database yet");
    }
    return SchemaUtility.getSchemaAction(ACTION_COMMON + num);
  }

  @Override
  protected URL getSchemaScript(String prefix, String num) {
    return JSureSchemaUtility.class.getResource(SQL_SCRIPT_PREFIX + prefix + 
                                                SEPARATOR + num + SQL_SCRIPT_SUFFIX);
  }
}
