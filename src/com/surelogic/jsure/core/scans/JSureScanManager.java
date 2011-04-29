package com.surelogic.jsure.core.scans;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.fluid.javac.persistence.*;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

import edu.cmu.cs.fluid.ide.IDEPreferences;

public class JSureScanManager {
	private static final JSureScanManager prototype = new JSureScanManager();
	
	public static JSureScanManager getInstance() {
		return prototype;
	}
	
	private final List<IJSureScanManagerListener> listeners = new CopyOnWriteArrayList<IJSureScanManagerListener>();
	private JSureData data;
	
	private JSureScanManager() {
		final File dataDir = JSurePreferencesUtility.getJSureDataDirectory();
		data = JSureDataDirScanner.scan(dataDir);
	}
	
	public void addListener(IJSureScanManagerListener l) {
		listeners.add(l);
	}
	
	public void removeListener(IJSureScanManagerListener l) {
		listeners.remove(l);
	}
	
	private void notify(DataDirStatus s, File dir) {
		for(IJSureScanManagerListener l : listeners) {
			l.updateScans(s, dir);
		}		
	}
	
	public synchronized JSureData getData() {
		return data;
	}
	
	public void addedScan(File run) {		
		if (run == null || !run.isDirectory()) {
			throw new IllegalArgumentException("Bad scan directory: "+run);
		}
		synchronized (this) {
			if (!data.getDataDir().equals(run.getParentFile())) {
				throw new IllegalArgumentException("Scan directory is not under the JSure data dir: "+run);
			}
			data = JSureDataDirScanner.scan(data.getDataDir());
		}
		notify(DataDirStatus.ADDED, run);
	}
	
	public void removedScans() {
		File dir;
		synchronized (this) {
			dir = data.getDataDir();
			data = JSureDataDirScanner.scan(data.getDataDir());
		}
		notify(DataDirStatus.CHANGED, dir);
	}
	
	/**
	 * Sets the JSure data directory to an existing directory.
	 * <p>
	 * This method simply changes the preference it does not move data from the
	 * old directory (or even delete the old directory).
	 * 
	 * @param dir
	 *            the new JSure data directory (must exist and be a directory).
	 * 
	 * @throws IllegalArgumentException
	 *             if the passed {@link File} is not a directory or doesn't
	 *             exist.
	 */
	// TODO what about the changes via the preference page?
	public void setJSureDataDirectory(final File dir) {
		if (dir != null && dir.isDirectory()) {
			synchronized (this) {
				if (dir.equals(data.getDataDir())) {
					// Ignoring, since it's the same as the current data dir
					return;
				}			
				EclipseUtility.setStringPreference(
						IDEPreferences.JSURE_DATA_DIRECTORY, dir.getAbsolutePath());
				data = JSureDataDirScanner.scan(dir);
			}
			notify(DataDirStatus.CHANGED, dir);
		} else {
			throw new IllegalArgumentException("Bad JSure data directory "
					+ dir + " it doesn't exist on the disk");
		}
	}
}
