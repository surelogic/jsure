package com.surelogic.jsure.core.scans;

import java.io.File;

public interface IJSureScanListener {
	void updateScans(DataDirStatus s, File dir);
}
