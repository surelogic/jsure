package com.surelogic.jsecure.client.eclipse;

import java.io.File;

import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.jsecure.client.eclipse.adhoc.OrientDBConnection;

public class JSecureScan {
	final File scanDir;
	
	public JSecureScan(File dir) {
		scanDir = dir;
	}
	
	public String getId() {
		return scanDir.getName();
	}
	
	public DBConnection getDB() {
	    return OrientDBConnection.getInstance(scanDir);
	}
}
