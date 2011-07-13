package com.surelogic.jsure.core.scans;

import static com.surelogic.jsure.core.preferences.JSurePreferencesUtility.BASELINE_SCAN;
import static com.surelogic.jsure.core.preferences.JSurePreferencesUtility.CURRENT_SCAN;
import static com.surelogic.jsure.core.preferences.JSurePreferencesUtility.SCAN_UPDATE_MODE;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.surelogic.common.core.EclipseUtility;

/**
 * Manages information about a baseline and a current scan.
 */
public final class JSureScansHub {

	private static final JSureScansHub INSTANCE = new JSureScansHub();

	private static final File USE_PREV = new File(
			"A dummy for the current scan");

	public static JSureScansHub getInstance() {
		return INSTANCE;
	}

	private JSureScansHub() {
		// Singleton
	}

	private final List<IJSureScanListener> f_listeners = new CopyOnWriteArrayList<IJSureScanListener>();

	public final void addListener(IJSureScanListener l) {
		f_listeners.add(l);
	}

	public final void removeListener(IJSureScanListener l) {
		f_listeners.remove(l);
	}

	/**
	 * Requires synchronization to be done by the caller
	 * 
	 * @param current
	 *            If true, get the current dir; otherwise, get the baseline dir
	 */
	private File getScanDir(boolean current) {
		final String dir = EclipseUtility
				.getStringPreference(current ? CURRENT_SCAN : BASELINE_SCAN);
		return new File(dir);
	}

	/**
	 * Requires synchronization to be done by the caller
	 * 
	 * @return true if changed
	 */
	private ScanStatus persistScans(String baseline, String current) {
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

	private String checkIfValid(File run) {
		if (run != null) {
			if (!run.isDirectory()) {
				throw new IllegalArgumentException("Not a directory: " + run);
			}
			// TODO check if valid run
			return run.getAbsolutePath();
		}
		return null;
	}

	public final void setBaselineScan(File prev) {
		setScans(prev, null);
	}

	public final void setCurrentScan(File curr) {
		setScans(null, curr);
	}

	public final void setScans(File baseline, File current) {
		String currentPath = checkIfValid(current);
		String baselinePath;

		ScanStatus status = ScanStatus.NEITHER_CHANGED;
		synchronized (this) {
			if (baseline == USE_PREV) {
				baselinePath = currentInfo == null ? null : currentInfo
						.getLocation().getAbsolutePath();
			} else {
				baselinePath = checkIfValid(baseline);
			}
			status = persistScans(baselinePath, currentPath);

			if (status.baselineChanged()) {
				baselineInfo = null;
			}
			if (status.currentChanged()) {
				currentInfo = null;
			}
		}
		if (status.changed()) {
			for (IJSureScanListener l : f_listeners) {
				l.scansChanged(status);
			}
		}
	}

	private JSureScanInfo baselineInfo, currentInfo;

	private synchronized JSureScanInfo getScanInfo(boolean current) {
		JSureScanInfo info = current ? currentInfo : baselineInfo;
		if (info == null) {
			File dir = getScanDir(current);
			if (dir != null && dir.isDirectory()) {
				if (current) {
					info = currentInfo = new JSureScanInfo(dir);
				} else {
					info = baselineInfo = new JSureScanInfo(dir);
				}
			}
		} else {
			// Check if it's still good
			File dir = info.getLocation();
			if (dir == null || !dir.isDirectory()) {
				return null;
			}
		}
		return info;
	}

	public final JSureScanInfo getBaselineScanInfo() {
		return getScanInfo(false);
	}

	public final JSureScanInfo getCurrentScanInfo() {
		return getScanInfo(true);
	}

	private ScanUpdateMode getUpdateMode() {
		String val = EclipseUtility.getStringPreference(SCAN_UPDATE_MODE);
		return ScanUpdateMode.valueOf(val);
	}

	public void gotNewScan(File runDir) {
		final ScanUpdateMode mode = getUpdateMode();
		switch (mode) {
		case DIFF_WITH_PREV:
			setScans(USE_PREV, runDir);
			break;
		case DIFF_WITH_BASELINE:
			setCurrentScan(runDir);
			break;
		case NO_UPDATE:
		default:
			return; // nothing to do
		}
	}
}
