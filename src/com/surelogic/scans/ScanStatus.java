package com.surelogic.scans;

public enum ScanStatus {
	NEITHER_CHANGED(false,false), BASELINE_CHANGED(true,false), CURRENT_CHANGED(false,true), BOTH_CHANGED(true,true);
	
	private final boolean baselineChanged, currentChanged;
	
	ScanStatus(boolean baseline, boolean current) {
		baselineChanged = baseline;
		currentChanged = current;
	}
	
	public boolean changed() {
		return baselineChanged || currentChanged;
	}
	
	public boolean baselineChanged() {
		return baselineChanged;
	}
	
	public boolean currentChanged() {
		return currentChanged;
	}
	
	public ScanStatus combine(ScanStatus other) {
		if (other == null) {
			return this;
		}
		if (baselineChanged || other.baselineChanged) {
			if (currentChanged || other.currentChanged) {
				return BOTH_CHANGED;
			} else {
				return BASELINE_CHANGED;
			}
		}
		// Baseline didn't change
		if (currentChanged || other.currentChanged) {
			return CURRENT_CHANGED;
		}
		return NEITHER_CHANGED;
	}
}
