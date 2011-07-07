package com.surelogic.javac.scans;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages info about the baseline and current scans
 * 
 * @author Edwin
 */
public abstract class JSureScansHub {
	private static JSureScansHub instance;
	private static final File USE_PREV = new File("this is a dummy marker for the current scan");
	
	public static synchronized JSureScansHub getInstance() {
		if (instance == null) {
			throw new IllegalStateException("Instance not initialized yet");
		}
		return instance;
	}	
	
	protected static synchronized void setInstance(JSureScansHub h) {
		if (h == null) {
			throw new IllegalArgumentException("New instance is null");
		}
		if (instance != null) {
			throw new IllegalStateException("Instance already initialized yet");
		}
		instance = h;
	}
	
	protected JSureScansHub() {
		// Nothing to do yet
	}
	
	private final List<IJSureScanListener> listeners = new CopyOnWriteArrayList<IJSureScanListener>();
	
	public final void addListener(IJSureScanListener l) {
		listeners.add(l);
	}
	
	public final void removeListener(IJSureScanListener l) {
		listeners.remove(l);
	}
	
 	/**
 	 * Requires synchronization to be done by the caller
 	 * 
 	 * @param current If true, get the current dir; otherwise, get the baseline dir
 	 */
	protected abstract File getScanDir(boolean current);
	
	/**
	 * Requires synchronization to be done by the caller
	 *  
	 * @return true if changed
	 */
	protected abstract ScanStatus persistScans(String baseline, String current);

	private String checkIfValid(File run) {
		if (run != null) {
			if (!run.isDirectory()) {
				throw new IllegalArgumentException("Not a directory: "+run);
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
				baselinePath = currentInfo == null ? null : currentInfo.getLocation().getAbsolutePath();
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
			for(IJSureScanListener l : listeners) {
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
		}
		return info;
	}
	
	public final JSureScanInfo getBaselineScanInfo() {
		return getScanInfo(false);
	}
	
	public final JSureScanInfo getCurrentScanInfo() {
		return getScanInfo(true);
	}

	protected abstract ScanUpdateMode getUpdateMode();
	
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
