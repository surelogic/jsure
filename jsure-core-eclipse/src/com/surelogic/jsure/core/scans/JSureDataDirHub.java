package com.surelogic.jsure.core.scans;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.surelogic.Nullable;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.ThreadSafe;
import com.surelogic.Unique;
import com.surelogic.common.FileUtility;
import com.surelogic.common.Pair;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.javac.persistence.JSureDataDir;
import com.surelogic.javac.persistence.JSureDataDirScanner;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

/**
 * Singleton that provides a notification hub about the scans within the JSure
 * data directory.
 * <p>
 * Clients can register with this hub and received notifications of when the
 * contents of the JSure data directory change.
 */
@ThreadSafe
@RegionLock("StateLock is f_lock protects f_dataDir")
public final class JSureDataDirHub {

  /**
   * Listens for changes to the set of scans within a JSure data directory.
   */
  public static interface ContentsChangeListener {
    /**
     * Notification that the scan contents of the JSure data directory has
     * changed. This could be because a new scan completed or old scans were
     * deleted.
     * 
     * @param dataDir
     *          the JSure data directory.
     */
    void scanContentsChanged(JSureDataDir dataDir);
  }

  private final List<ContentsChangeListener> f_contentsListeners = new CopyOnWriteArrayList<ContentsChangeListener>();

  public void addContentsChangeListener(ContentsChangeListener l) {
    f_contentsListeners.add(l);
  }

  public void removeContentsChangeListener(ContentsChangeListener l) {
    f_contentsListeners.remove(l);
  }

  /**
   * Listens for changes to the current scan of focus. The current scan of focus
   * is selected in the user interface, but could also be changed because a scan
   * is added or deleted.
   */
  public static interface CurrentScanChangeListener {
    /**
     * Notification that the current scan of focus has changed.
     * <p>
     * This notification is largely orthogonal to changes in the contents of the
     * JSure data directory but could be due to a new scan being completed.
     * 
     * @param scan
     *          the current scan of focus, which may be {@code null} to indicate
     *          no current scan of focus.
     */
    void currentScanChanged(JSureScan scan);
  }

  private final List<CurrentScanChangeListener> f_currentScanListeners = new CopyOnWriteArrayList<CurrentScanChangeListener>();

  public void addCurrentScanChangeListener(CurrentScanChangeListener l) {
    f_currentScanListeners.add(l);
  }

  public void removeCurrentScanChangeListener(CurrentScanChangeListener l) {
    f_currentScanListeners.remove(l);
  }

  /**
   * Listens for the addition of new scans to the JSure data directory. This
   * call only works for scans started within the Eclipse IDE (no notifications
   * for scans added by Ant).
   */
  public static interface NewScanListener {
    /**
     * Notification that a new scan exists in the JSure data directory.
     * 
     * @param scan
     *          the new scan.
     */
    void newScan(JSureScan scan);
  }

  private final List<NewScanListener> f_newScanListeners = new CopyOnWriteArrayList<NewScanListener>();

  public void addNewScanListener(NewScanListener l) {
    f_newScanListeners.add(l);
  }

  public void removeNewScanListener(NewScanListener l) {
    f_newScanListeners.remove(l);
  }

  /*
   * Singleton
   */

  private static final JSureDataDirHub INSTANCE = new JSureDataDirHub();

  public static JSureDataDirHub getInstance() {
    return INSTANCE;
  }

  /**
   * Protects the mutable state of this class.
   */
  private final Object f_lock = new Object();

  /*
   * Mutable state.
   */

  private JSureDataDir f_dataDir;
  private JSureScan f_currentScan = null;
  private JSureScanInfo f_currentScanInfo = null;

  @Unique
  private JSureDataDirHub() {
    final File dataDir = JSurePreferencesUtility.getJSureDataDirectory();
    f_dataDir = JSureDataDirScanner.scan(dataDir);
    loadCurrentScanPreference();
  }

  /**
   * Gets the current information about the JSure data directory.
   * 
   * @return current information about the JSure data directory.
   */
  public JSureDataDir getJSureDataDir() {
    synchronized (f_lock) {
      return f_dataDir;
    }
  }

  /**
   * Informs this hub that a new scan has completed and the contents of the
   * JSure data directory have changed. This will cause notification to
   * registered listeners.
   * 
   * @param newScanDir
   *          the new scan directory.
   */
  public void scanDirectoryAdded(final File newScanDir) {
    if (newScanDir == null || !newScanDir.isDirectory()) {
      throw new IllegalArgumentException(I18N.err(231, newScanDir, "null or not a directory"));
    }
    scanDirectoryChangedHelper(newScanDir);
  }

  /**
   * Notifies this hub that one or more scan directories have been deleted. This
   * will cause notification to registered listeners.
   * <p>
   * Probably should only be called from a job obtained from
   * {@link #getDeleteScansJob(List)}.
   */
  public void scanDirectoryOrDirectoriesDeleted() {
    scanDirectoryChangedHelper(null);
  }

  private void scanDirectoryChangedHelper(final File optionalNewScanDir) {
    JSureDataDir dataDir = null;
    boolean currentScanChanged = false;
    JSureScan newScan = null;
    synchronized (f_lock) {
      if (f_dataDir == null) {
        throw new IllegalStateException(I18N.err(227));
      }

      f_dataDir = JSureDataDirScanner.scan(f_dataDir);
      dataDir = f_dataDir; // cannot hold the lock when we notify

      /*
       * It is possible that the current scan no longer exists.
       */
      if (f_currentScan != null) {
        if (!f_dataDir.contains(f_currentScan)) {
          /*
           * We deleted the current scan so it must be cleared. We change it
           * here, to have a legal value, and notify listeners of the change
           * below.
           */
          setCurrentScanHelper(null);
          currentScanChanged = true; // notify below
        }
      }
      if (optionalNewScanDir != null) {
        newScan = dataDir.findScan(optionalNewScanDir);
        if (newScan != null) {
          /*
           * If we found the passed new scan so we set it as the current scan of
           * interest and notify the listeners of the change below.
           */
          setCurrentScanHelper(newScan);
          currentScanChanged = true; // notify below
        }
      }
    }
    // notify registered listeners
    for (ContentsChangeListener l : f_contentsListeners)
      l.scanContentsChanged(dataDir);
    if (currentScanChanged) {
      for (CurrentScanChangeListener l : f_currentScanListeners)
        l.currentScanChanged(newScan);
      if (newScan != null) {
        for (NewScanListener l : f_newScanListeners)
          l.newScan(newScan);
      }
    }
  }

  /**
   * Obtains a job that will, when run, delete the passed list of scans from the
   * JSure directory.
   * 
   * @param scansToDelete
   *          the list of JSure scans to delete from the JSure directory. This
   *          list should not be empty.
   * @return a job that can be scheduled to perform the scan deletion.
   */
  public SLJob getDeleteScansJob(final List<JSureScan> scansToDelete) {
    final SLJob job = new AbstractSLJob("Deleting JSure Scans...") {

      @Override
      public SLStatus run(SLProgressMonitor monitor) {
        monitor.begin(scansToDelete.size() + 1);
        try {
          for (JSureScan scan : scansToDelete) {
            final File dir = scan.getDir();
            if (!FileUtility.recursiveDelete(dir)) {
              return SLStatus.createErrorStatus(233, I18N.err(233, dir.getAbsolutePath()));
            }
            monitor.worked(1);
          }
        } finally {
          JSureDataDirHub.getInstance().scanDirectoryOrDirectoriesDeleted();
          monitor.worked(1);
          monitor.done();
        }
        return SLStatus.OK_STATUS;
      }
    };
    return job;
  }

  /**
   * Gets the current scan of focus.
   * 
   * @return the current scan of focus, which may be {@code null} to indicate no
   *         current scan of focus.
   */
  public JSureScan getCurrentScan() {
    synchronized (f_lock) {
      return f_currentScan;
    }
  }

  /**
   * Gets the information about the current scan of focus.
   * 
   * @return the cached loaded information about the current scan of focus,
   *         which may be {@code null} to indicate no current scan of focus.
   */
  public JSureScanInfo getCurrentScanInfo() {
    synchronized (f_lock) {
      return f_currentScanInfo;
    }
  }

  /**
   * Gets a report of the difference between the current scan and the last fully
   * compatible scan of the same set of projects.
   * 
   * @return a pair comprising of the scan differences (first) and the scan info
   *         for the old scan (second), or {@code null} if no compatible old
   *         scan could be located.
   */
  public Pair<ScanDifferences, JSureScanInfo> getDifferencesBetweenCurrentScanAndLastCompatibleScanOrNull() {
    return null;
  }

  /**
   * Sets the current scan of focus. This will cause notification to registered
   * listeners.
   * 
   * @param value
   *          the new scan to focus on, or {@code null} to focus on no scan.
   */
  public void setCurrentScan(final JSureScan value) {
    synchronized (f_lock) {
      /*
       * Bail if the scan is already set to this. Avoiding a whole lot of double
       * calls if UI code goes bad.
       */
      if (value == f_currentScan)
        return;

      setCurrentScanHelper(value);
    }
    for (CurrentScanChangeListener l : f_currentScanListeners) {
      l.currentScanChanged(value);
    }
  }

  /**
   * Internal helper method to set the current scan value. It sets the current
   * scan and generates info for it.
   * 
   * @param value
   *          the scan to set or {@code null} to clear the current scan value.
   */
  @RequiresLock("StateLock")
  private void setCurrentScanHelper(@Nullable final JSureScan value) {
    if (value != null) {
      if (!f_dataDir.contains(value))
        throw new IllegalArgumentException(I18N.err(232, value, f_dataDir.getDir().getAbsoluteFile()));
    }
    f_currentScan = value;
    if (value == null)
      f_currentScanInfo = null;
    else
      f_currentScanInfo = new JSureScanInfo(value);
    saveCurrentScanPreference();
  }

  /*
   * Persistence of the user's current scan selection as a preference.
   */

  private static final String NONE = "(NONE)";

  private void loadCurrentScanPreference() {
    JSureScan currentScan = null;
    String value = EclipseUtility.getStringPreference(JSurePreferencesUtility.CURRENT_SCAN);
    synchronized (f_lock) {
      if (value != null && !NONE.equals(value)) {
        final File scanDir = new File(value);
        currentScan = f_dataDir.findScan(scanDir);
      }
      setCurrentScanHelper(currentScan);
    }
  }

  private void saveCurrentScanPreference() {
    final JSureScan current = getCurrentScan();
    String value = NONE;
    if (current != null) {
      value = current.getDir().getPath();
    }
    EclipseUtility.setStringPreference(JSurePreferencesUtility.CURRENT_SCAN, value);
  }
}
