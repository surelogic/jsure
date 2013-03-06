/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionCursor.java,v 1.9 2007/07/05 18:15:13 aarong Exp $ */
package edu.cmu.cs.fluid.version;

import java.util.Observable;
import java.util.Observer;

/** A pointer to a version which tracks any changes to it.
 * If the version associated with a cursor get a new child,
 * the cursor moves to this new version.  A cursor is active
 * if its associated version is the current one, the one
 * that gets any slot changes.
 * <p> A version cursor can have observers, which are notified
 * if the tracked version changes.
 * <p> <b></b>
 * @deprecated use <tt>VersionTracker</tt> instead.
 */
@Deprecated
public class VersionCursor extends VersionTracker {
  private Version tracking;

  /** Create a version cursor for the current version.
   * @deprecated use {@link VersionTracker} instances instead,
   * and explicitly update them when transaction is complete.
   */
  @Deprecated
  public VersionCursor() {
    this(Version.getVersionLocal());
  }

  /** Create a version cursor for the specified version
   * @deprecated use {@link VersionTracker} instances instead,
   * and explicitly update them when transaction is complete.
   */
  @Deprecated
  public VersionCursor(Version v) {
    tracking = v;
    v.addCursor(this);
  }

  @Override
  public void notifyObservers() {
    setChanged();
    super.notifyObservers();
  }

  /** Tracked version gets a new child.
   * Called only from class <tt>Version</tt>
   */
  synchronized void moveCursor( final Version v ) {
    final Version old = tracking;
    tracking = v;
    notifyObservers();
    fireVersionTrackerEvent( old, v );
  }

  @Override
  public synchronized Version getVersion() {
    tracking.freeze();
    return tracking;
  }

  @Override
  public synchronized void setVersion( final Version v ) {
    tracking.removeCursor(this);
    final Version old = tracking;
    tracking = v;
    v.addCursor(this);
    notifyObservers();
    fireVersionTrackerEvent( old, v );
  }

  /** Make this cursor's tracked version the current version.
   * @deprecated use Version.setVersion(this.getVersion())
   */
  @Override
  @Deprecated
  public void makeActive() {
    Version.setVersion(tracking);
  }

  /** Return true if the current version is being tracked by this cursor. */
  @Override
  public boolean isActive() {
    return tracking.isCurrent();
  }

  /** Attach an observable to a versioned subject.
   * The observer will only be notified when
   * the cursor is active, that is if the change is in
   * the tracked version.
   * @deprecated attach yourself to a version tracker instead,
   * and then ask the subject when the version tracker updates.
   */
  @Deprecated
  public void attachObserver(Observable subject, final Observer watcher) {
    subject.addObserver(new Observer() {
      @Override
      public void update(Observable s, Object arg) {
	if (isActive()) watcher.update(s,arg);
      }
    });
  }
}
