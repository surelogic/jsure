package com.surelogic.javac.scans;

/**
 * Listens to whether the baseline/current scans have changed
 * 
 * @author Edwin
 */
public interface IJSureScanListener {
	void scansChanged(ScanStatus status);
}
