package com.surelogic.jsure.core.scans;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.surelogic.RegionLock;
import com.surelogic.ThreadSafe;
import com.surelogic.Unique;
import com.surelogic.javac.persistence.JSureDataDir;
import com.surelogic.javac.persistence.JSureDataDirScanner;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

/**
 * Singleton that provides a notification hub about the scans or runs within the
 * JSure data directory.
 * <p>
 * Clients can register with this hub and received notifications of when the
 * contents of the JSure data directory change due to a scan being added or
 * deleted.
 */
@ThreadSafe
@RegionLock("StateLock is f_lock protects f_dataDir")
public final class JSureDataDirHub {

	/**
	 * Events that can occur within the JSure data directory.
	 */
	public static enum Status {
		UNCHANGED, ADDED, CHANGED
	}

	/**
	 * Listens for changes to the set of scans or runs within a JSure data
	 * directory.
	 */
	public static interface Listener {
		/**
		 * Notification of a change to the JSure data directory.
		 * 
		 * @param event
		 *            what happened within the JSure data directory. The value
		 *            of {@link Status#UNCHANGED} indicates nothing changed.
		 * @param directory
		 *            the JSure data directory if {@link Status#CHANGED} or the
		 *            new scan directory if {@link Status#ADDED}.
		 */
		void updateScans(Status event, File directory);
	}

	private static final JSureDataDirHub INSTANCE = new JSureDataDirHub();

	public static JSureDataDirHub getInstance() {
		return INSTANCE;
	}

	private final List<Listener> f_listeners = new CopyOnWriteArrayList<Listener>();

	public void addListener(Listener l) {
		f_listeners.add(l);
	}

	public void removeListener(Listener l) {
		f_listeners.remove(l);
	}

	private void notify(Status s, File dir) {
		for (Listener l : f_listeners) {
			l.updateScans(s, dir);
		}
	}

	/**
	 * Protects mutable state.
	 */
	private final Object f_lock = new Object();

	/*
	 * Mutable state.
	 */

	private JSureDataDir f_dataDir;

	@Unique
	private JSureDataDirHub() {
		final File dataDir = JSurePreferencesUtility.getJSureDataDirectory();
		f_dataDir = JSureDataDirScanner.scan(dataDir);
	}

	public JSureDataDir getJSureDataDir() {
		synchronized (f_lock) {
			return f_dataDir;
		}
	}

	public void scanDirectoryAdded(final File newScanDir) {
		if (newScanDir == null || !newScanDir.isDirectory()) {
			throw new IllegalArgumentException("Bad scan directory: "
					+ newScanDir);
		}
		synchronized (f_lock) {
			if (f_dataDir == null) {
				throw new IllegalStateException("No scan data");
			} else if (f_dataDir.getDir() == null) {
				throw new IllegalStateException("No data dir");
			}
			if (!f_dataDir.getDir().equals(newScanDir.getParentFile())) {
				throw new IllegalArgumentException(
						"Scan directory is not under the JSure data dir: "
								+ newScanDir);
			}
			f_dataDir = JSureDataDirScanner.scan(f_dataDir);
			final JSureScan scan = f_dataDir.findScan(newScanDir);
		}
		notify(Status.ADDED, newScanDir);
	}

	public void scanDirectoryOrDirectoriesDeleted() {
		final File dir;
		synchronized (f_lock) {
			dir = f_dataDir.getDir();
			f_dataDir = JSureDataDirScanner.scan(f_dataDir);
		}
		notify(Status.CHANGED, dir);
	}
}
