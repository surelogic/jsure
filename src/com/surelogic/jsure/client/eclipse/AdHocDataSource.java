package com.surelogic.jsure.client.eclipse;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;

import com.surelogic.common.adhoc.IAdHocDataSource;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.logging.SLLogger;

public final class AdHocDataSource implements IAdHocDataSource {

	public DBConnection getDB() {
		return Data.getInstance();
	}

	public int getMaxRowsPerQuery() {
		return 5000;
	}

	public File getQuerySaveFile() {
		return new File(Activator.getDefault().getLocation("queries.xml"));
	}

	public URL getDefaultQueryUrl() {
		return null;
	}

	public void badQuerySaveFileNotification(Exception e) {
		SLLogger.getLogger().log(Level.SEVERE,
				I18N.err(4, getQuerySaveFile().getAbsolutePath()), e);
	}

}
