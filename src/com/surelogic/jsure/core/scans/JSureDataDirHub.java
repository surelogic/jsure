package com.surelogic.jsure.core.scans;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.javac.persistence.JSureDataDir;
import com.surelogic.javac.persistence.JSureDataDirScanner;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

import edu.cmu.cs.fluid.ide.IDEPreferences;

/**
 * Singleton that provides a notification hub about the scans or runs within the
 * JSure data directory.
 * <p>
 * Clients can register with this hub and received notifications of when the
 * contents of the JSure data directory change due to a scan being added or
 * deleted.
 */
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

	private JSureDataDir f_dataDir;

	private JSureDataDirHub() {
		final File dataDir = JSurePreferencesUtility.getJSureDataDirectory();
		f_dataDir = JSureDataDirScanner.scan(dataDir);
	}

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

	public JSureDataDir getJSureDataDir() {
		synchronized (this) {
			return f_dataDir;
		}
	}

	public void notifyScanAdded(final File run) {
		if (run == null || !run.isDirectory()) {
			throw new IllegalArgumentException("Bad scan directory: " + run);
		}
		synchronized (this) {
			if (f_dataDir == null) {
				throw new IllegalStateException("No scan data");
			} else if (f_dataDir.getDir() == null) {
				throw new IllegalStateException("No data dir");
			}
			if (!f_dataDir.getDir().equals(run.getParentFile())) {
				throw new IllegalArgumentException(
						"Scan directory is not under the JSure data dir: "
								+ run);
			}
			f_dataDir = JSureDataDirScanner.scan(f_dataDir);
		}
		notify(Status.ADDED, run);
	}

	public void notifyScanRemoved() {
		final File dir;
		synchronized (this) {
			dir = f_dataDir.getDir();
			f_dataDir = JSureDataDirScanner.scan(f_dataDir);
		}
		notify(Status.CHANGED, dir);
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
				if (dir.equals(f_dataDir.getDir())) {
					// Ignoring, since it's the same as the current data dir
					return;
				}
				EclipseUtility.setStringPreference(
						IDEPreferences.JSURE_DATA_DIRECTORY,
						dir.getAbsolutePath());
				f_dataDir = JSureDataDirScanner.scan(dir);
			}
			notify(Status.CHANGED, dir);
		} else {
			throw new IllegalArgumentException("Bad JSure data directory "
					+ dir + " it doesn't exist on the disk");
		}
	}
}
