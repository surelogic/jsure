package com.surelogic.jsure.core.scans;

import java.io.File;

/**
 * Listens for changes to scans in the JSure data directory
 * 
 * @author Edwin
 */
public interface IJSureScanManagerListener {
	void updateScans(DataDirStatus s, File dir);
}
