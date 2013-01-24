package com.surelogic.jsure.core.scans;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.surelogic.Nullable;
import com.surelogic.RegionLock;
import com.surelogic.ThreadSafe;
import com.surelogic.Unique;
import com.surelogic.common.FileUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.dropsea.irfree.ISeaDiff;
import com.surelogic.dropsea.irfree.SeaSnapshot;
import com.surelogic.javac.persistence.JSureDataDir;
import com.surelogic.javac.persistence.JSureDataDirScanner;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.preferences.UninterestingPackageFilterUtility;

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
  private final Object f_lock = new Object() {};

  /*
   * Mutable state.
   */

  private JSureDataDir f_dataDir;
  private JSureScan f_currentScan = null;
  private JSureScanInfo f_currentScanInfo = null;
  private JSureScanInfo f_lastMatchingScanInfo = null;
  private ScanDifferences f_scanDiff = null;

  @Unique
  private JSureDataDirHub() {
    final File dataDir = JSurePreferencesUtility.getJSureDataDirectory();
    f_dataDir = JSureDataDirScanner.scan(dataDir);
    if (!f_dataDir.isEmpty())
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
    SLJob job = null;
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
          job = getSetCurrentScanJob(null, true, true, false);
        }
      }
      if (optionalNewScanDir != null) {
        newScan = dataDir.findScan(optionalNewScanDir);
        if (newScan != null) {
          /*
           * If we found the passed new scan so we set it as the current scan of
           * interest and notify the listeners of the change below.
           */
          job = getSetCurrentScanJob(newScan, true, true, true);
        }
      }
    }
    if (job != null)
      EclipseUtility.toEclipseJob(job).schedule();
    else
      notifyListeners(true, false, false);
  }

  private void notifyListeners(boolean notifyContentsChanged, boolean notifyCurrentScanChanged, boolean isNewScan) {
    final JSureDataDir dataDir;
    final JSureScan currentScan;
    synchronized (f_lock) {
      dataDir = f_dataDir;
      currentScan = f_currentScan;
    }
    // notify registered listeners
    if (notifyContentsChanged)
      for (ContentsChangeListener l : f_contentsListeners)
        l.scanContentsChanged(dataDir);
    if (notifyCurrentScanChanged) {
      for (CurrentScanChangeListener l : f_currentScanListeners)
        l.currentScanChanged(currentScan);
      if (isNewScan) {
        for (NewScanListener l : f_newScanListeners)
          l.newScan(currentScan);
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
            if (!FileUtility.recursiveDelete(dir, true)) {
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
   * Gets the information about the last scan that matches the scan of focus.
   * 
   * @return the cached loaded information about the last scan that matches the
   *         current scan of focus, which may be {@code null} to indicate no no
   *         such scan could be found.
   */
  public JSureScanInfo getLastMatchingScanInfo() {
    synchronized (f_lock) {
      return f_lastMatchingScanInfo;
    }
  }

  /**
   * Gets a report of the difference between the current scan and the last fully
   * compatible scan of the same set of projects.
   * 
   * @return the differences between {@link #getCurrentScan()} and
   *         {@link #getLastMatchingScanInfo()}, or {@code null} if the report
   *         could not be computed, probably because no last matching scan
   *         exists.
   */
  public ScanDifferences getDifferencesBetweenCurrentScanAndLastCompatibleScanOrNull() {
    synchronized (f_lock) {
      return f_scanDiff;
    }
  }

  /**
   * Sets the current scan of focus. This will cause notification to registered
   * listeners.
   * 
   * @param value
   *          the new scan to focus on, or {@code null} to focus on no scan.
   */
  public void setCurrentScan(final JSureScan value) {
    final SLJob job;
    synchronized (f_lock) {
      /*
       * Bail if the scan is already set to this. Avoiding a whole lot of double
       * calls if UI code goes bad.
       */
      if (value == f_currentScan)
        return;

      job = getSetCurrentScanJob(value, false, true, false);
    }
    EclipseUtility.toEclipseJob(job).schedule();
  }

  /**
   * Internal helper method to get a job to set the current scan value. It sets
   * the current scan and generates info for it.
   * 
   * @param jsureScan
   *          the scan to set or {@code null} to clear the current scan value.
   */
  private SLJob getSetCurrentScanJob(@Nullable final JSureScan jsureScan, final boolean notifyContentsChanged,
      final boolean notifyCurrentScanChanged, final boolean isNewScan) {
    return new AbstractSLJob(jsureScan == null ? "Clearing the JSure Scan..." : "Loading a JSure Scan from "
        + jsureScan.getDirName()) {
      public SLStatus run(final SLProgressMonitor monitor) {
        monitor.begin(4);
        try {
          synchronized (f_lock) {
            if (jsureScan != null) {
              if (!f_dataDir.contains(jsureScan)) {
                final int code = 232;
                return SLStatus.createErrorStatus(code, I18N.err(code, jsureScan, f_dataDir.getDir().getAbsoluteFile()));
              }
            }
            final SeaSnapshot loader = SeaSnapshot.create();
            JSureScanInfo currentScanInfo = null;
            JSureScanInfo lastMatchingScanInfo = null;
            ScanDifferences scanDiff = null;
            monitor.worked(1);
            if (jsureScan != null) {
              currentScanInfo = new JSureScanInfo(jsureScan, loader);
              currentScanInfo.getDropInfo(); // force loading

              JSureScan last = null;
              if (XUtil.useExperimental) {
                last = OracleUtility.findOracle(currentScanInfo.getJSureRun());
              }
              if (last == null) {
                last = f_dataDir.findLastMatchingScan(currentScanInfo.getJSureRun());
              }
              monitor.worked(1);
              if (monitor.isCanceled()) {
                loader.clear();
                return SLStatus.CANCEL_STATUS;
              }
              if (last != null) {
                lastMatchingScanInfo = new JSureScanInfo(last, loader);
                lastMatchingScanInfo.getDropInfo(); // force loading
                loader.clear(); // here because of forced loading
                monitor.worked(1);
                if (monitor.isCanceled()) {
                  // loader.clear();
                  return SLStatus.CANCEL_STATUS;
                }
                final ISeaDiff diff = currentScanInfo.diff(lastMatchingScanInfo,
                    UninterestingPackageFilterUtility.UNINTERESTING_PACKAGE_FILTER);
                // loader.clear(); // here because of lazy load
                monitor.worked(1);
                if (monitor.isCanceled()) {
                  return SLStatus.CANCEL_STATUS;
                }
                scanDiff = diff.build();
                monitor.worked(1);
              }
            }
            f_currentScan = jsureScan;
            f_currentScanInfo = currentScanInfo;
            f_lastMatchingScanInfo = lastMatchingScanInfo;
            f_scanDiff = scanDiff;
            saveCurrentScanPreference();
          }
          // without lock here for notification
          notifyListeners(notifyContentsChanged, notifyCurrentScanChanged, isNewScan);
        } finally {
          monitor.done();
        }
        return SLStatus.OK_STATUS;
      }
    };
  }

  /*
   * Persistence of the user's current scan selection as a preference.
   */

  private static final String NONE = "(NONE)";

  private void loadCurrentScanPreference() {
    JSureScan currentScan = null;
    String value = EclipseUtility.getStringPreference(JSurePreferencesUtility.CURRENT_SCAN);
    final JSureDataDir dataDir;
    synchronized (f_lock) {
      dataDir = f_dataDir;
    }
    if (value != null && !NONE.equals(value)) {
      final File scanDir = new File(value);
      currentScan = dataDir.findScan(scanDir);
    }
    if (currentScan != null) {
      final SLJob job = getSetCurrentScanJob(currentScan, false, true, false);
      EclipseUtility.toEclipseJob(job).schedule();
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
