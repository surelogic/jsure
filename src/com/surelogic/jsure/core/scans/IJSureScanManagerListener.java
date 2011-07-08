package com.surelogic.jsure.core.scans;

import java.io.File;

/**
 * Listens for changes to the set of scans or runs within a JSure data
 * directory.
 */
public interface IJSureScanManagerListener {
	/**
	 * Notification of a change to the JSure data directory.
	 * 
	 * @param s
	 *            the status
	 * @param dir
	 */
	void updateScans(DataDirStatus s, File dir);
}
