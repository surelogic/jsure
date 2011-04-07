package com.surelogic.jsure.core.preferences;

import java.io.File;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.fluid.javac.scans.*;

import static com.surelogic.jsure.core.preferences.JSurePreferencesUtility.*;

/**
 * Handles the Eclipse-specific details of persisting the scan info
 * 
 * @author Edwin
 */
public class JSureEclipseHub extends JSureScansHub {
	static {
		setInstance(new JSureEclipseHub());
	}
	
	public static void init() {
		// Create the instance above
	}
	
	private JSureEclipseHub() {
		// Nothing to do
	}
	
	@Override
	protected ScanStatus persistScans(String baseline, String current) {
		ScanStatus changed = ScanStatus.NEITHER_CHANGED;
		if (baseline != null) {
			if (update(BASELINE_SCAN, baseline)) {
				changed = changed.combine(ScanStatus.BASELINE_CHANGED);
			}
		}
		if (current != null) {
			if (update(CURRENT_SCAN, current)) {
				changed = changed.combine(ScanStatus.CURRENT_CHANGED);
			}
		}
		return changed;
	}

	private boolean update(String key, String value) {
		final String old = EclipseUtility.getStringPreference(key);
		if (!value.equals(old)) {
			// Changed
			EclipseUtility.setStringPreference(key, value);
			return true;
		}
		return false;
	}

	@Override
	protected File getScanDir(boolean current) {
		final String dir = EclipseUtility.getStringPreference(current ? CURRENT_SCAN : BASELINE_SCAN);
		return new File(dir);
	}

	@Override
	protected ScanUpdateMode getUpdateMode() {
		String val = EclipseUtility.getStringPreference(SCAN_UPDATE_MODE);
		return ScanUpdateMode.valueOf(val);
	}
}
