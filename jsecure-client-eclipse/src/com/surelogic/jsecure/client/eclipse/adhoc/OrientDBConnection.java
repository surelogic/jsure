package com.surelogic.jsecure.client.eclipse.adhoc;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import com.surelogic.common.jdbc.AbstractDBConnection;
import com.surelogic.common.jdbc.AbstractSchemaData;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.jsecure.client.eclipse.ClassSummarizer;

public class OrientDBConnection extends AbstractDBConnection {
	// TODO will this hold on to data?
    private static final ConcurrentHashMap<String, OrientDBConnection> INSTANCES = new ConcurrentHashMap<String, OrientDBConnection>();

	public static DBConnection getInstance(final File scanDir) {				
		final File dbLoc = new File(scanDir, ClassSummarizer.DB_PATH);
		final OrientDBConnection conn = new OrientDBConnection(dbLoc);
		final String absPath = dbLoc.getAbsolutePath();
		final OrientDBConnection old = INSTANCES.putIfAbsent(absPath, conn);
		if (old != null) {
			return old;
		} else {
			return conn;
		}
	}
	
	private final File dbLoc;
	
	public OrientDBConnection(File loc) {
		dbLoc = loc;
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		return new OrientConnection(dbLoc);
	}

	@Override
	public SchemaData getSchemaLoader() {
		return new AbstractSchemaData("com.surelogic.jsecure.schema",
				Thread.currentThread().getContextClassLoader(),
				SLLicenseProduct.EXEMPT) {
			@Override
			protected Object newInstance(String qname) 
					throws InstantiationException, IllegalAccessException, ClassNotFoundException {
				return loader.loadClass(qname).newInstance();
			}

			@Override
			public URL getSchemaResource(final String name) {
				return loader.getResource(getSchemaResourcePath(name));
			}

			@Override
			protected InputStream getResourceAsStream(String path) {
				return loader.getResourceAsStream(path);
			}
		};
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void loggedBootAndCheckSchema() {
		// TODO Auto-generated method stub
	}

	@Override
	public void bootAndCheckSchema() throws Exception {
		// TODO Auto-generated method stub
	}
}
