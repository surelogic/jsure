package com.surelogic.javac.scans;

public enum ScanUpdateMode {
	DIFF_WITH_PREV(true, true), DIFF_WITH_BASELINE(false,true), NO_UPDATE(false, false);
	
	private final boolean updateBaseline, updateCurrent;
	
	ScanUpdateMode(boolean updateB, boolean updateC) {
		updateBaseline = updateB;
		updateCurrent = updateC;
	}
	
	public boolean autoUpdateBaseline() {
		return updateBaseline;
	}
	public boolean autoUpdateCurrent() {
		return updateCurrent;
	}
}
