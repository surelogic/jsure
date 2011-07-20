package com.surelogic.jsure.core.scans;

import static com.surelogic.jsure.core.preferences.JSurePreferencesUtility.BASELINE_SCAN;
import static com.surelogic.jsure.core.preferences.JSurePreferencesUtility.CURRENT_SCAN;
import static com.surelogic.jsure.core.preferences.JSurePreferencesUtility.SCAN_UPDATE_MODE;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.*;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.javac.persistence.JSureRun;
import com.surelogic.jsure.core.scans.JSureDataDirHub.Status;

/**
 * Manages information about a baseline and a current scan.
 */
public final class JSureScansHub {

	public static enum ScanStatus {
		NEITHER_CHANGED(false, false), BASELINE_CHANGED(true, false), CURRENT_CHANGED(
				false, true), BOTH_CHANGED(true, true);

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

	public static interface Listener {
		void scansChanged(ScanStatus status);
	}

	private static final JSureScansHub INSTANCE = new JSureScansHub();
	static {
		JSureDataDirHub.getInstance().addListener(
				new JSureDataDirHub.Listener() {
					@Override
					public void updateScans(Status event, File directory) {
						switch (event) {
						case UNCHANGED:
							return;
						case ADDED:
							// Nothing to do, since the baseline/current scans
							// should be reset elsewhere
							break;
						case CHANGED:
							// Assume everything changed
							final SLJob job = new AbstractSLJob(
									"Updating baseline/current scans") {
								@Override
								public SLStatus run(SLProgressMonitor monitor) {
									getInstance().notifyListeners(
											ScanStatus.BOTH_CHANGED);
									return SLStatus.OK_STATUS;
								}
							};
							EclipseJob.getInstance().schedule(job);
							break;
						}
					}
				});
	}

	private static final File USE_PREV = new File(
			"A dummy for the current scan");

	public static JSureScansHub getInstance() {
		return INSTANCE;
	}

	private JSureScansHub() {
		// Singleton
	}

	private final List<Listener> f_listeners = new CopyOnWriteArrayList<Listener>();

	public final void addListener(Listener l) {
		f_listeners.add(l);
	}

	public final void removeListener(Listener l) {
		f_listeners.remove(l);
	}

	void notifyListeners(ScanStatus status) {
		for (Listener l : f_listeners) {
			l.scansChanged(status);
		}
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
						.getDir().getAbsolutePath();
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
			notifyListeners(status);
		}
	}

	private JSureScanInfo baselineInfo, currentInfo;

	private synchronized JSureScanInfo getScanInfo(boolean current) {
		JSureScanInfo info = current ? currentInfo : baselineInfo;
		if (info == null) {
			File dir = getScanDir(current);
			if (dir != null && dir.isDirectory()) {
				try {
					final JSureRun run = new JSureRun(dir);
					final JSureScanInfo runInfo = new JSureScanInfo(run);
					if (current) {
						info = currentInfo = runInfo;
					} else {
						info = baselineInfo = runInfo;
					}
				} catch (Exception e) {
					/*
					 * We failed to load up the information...log this problem
					 * as a warning.
					 */
					info = null;
					SLLogger.getLogger().log(Level.WARNING,
							I18N.err(227, dir.getAbsolutePath()), e);
				}
			}
		} else {
			/*
			 * Check if the directory still exists on the disk.
			 */
			File dir = info.getDir();
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
