/*
 * Created on Dec 7, 2007
 */
package com.surelogic.jsure.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public class JSureJDBCUtils {
  /**
   * Return the database type, based on what the JDBC metadata reports.
   * 
   * @param conn
   * @return
   * @throws SQLException
   */
  public static DBType getDb(Connection conn) throws SQLException {
    /*
    return "Oracle".equals(conn.getMetaData().getDatabaseProductName()) ? DBType.ORACLE
        : DBType.DERBY;
    */
    return DBType.DERBY;
  }  
}
