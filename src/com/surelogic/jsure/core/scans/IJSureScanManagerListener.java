package com.surelogic.jsure.core.scans;

import java.io.File;

public interface IJSureScanManagerListener {
	void updateScans(DataDirStatus s, File dir);
}
