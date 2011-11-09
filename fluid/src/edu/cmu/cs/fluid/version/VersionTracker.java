/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionTracker.java,v 1.13 2007/11/14 22:48:14 boyland Exp $ */
package edu.cmu.cs.fluid.version;

import java.util.Observable;
import java.util.Vector;

/** Instances with this interface track a version.
 * They may be used as a coordination point between
 * things that want to track the same version.
 * @see VersionMarker
 */
public abstract class VersionTracker extends Observable {
  private Vector<VersionTrackerListener> listeners = new Vector<VersionTrackerListener>();

  /** Get the current version being tracked.
   */
  public abstract Version getVersion();

  /** Set the version being tracked to this new version.
   * Notify observers of the change.
   * If the new version is constructed from the old version,
   * one should either <ol>
   * <li> lock the tracker during construction (pessimistic), or
   * <li> use {@link #moveFromVersionToCurrent} instead (optimisitic).
   * </ol>
   */
  public abstract void setVersion(Version v);

  /**
   * Execute thunk and move the version being tracked to the resulting version.
   * If another thread attempts to change this tracker while the thunk is
   * executing,  then the system tries again to run the thunk.
   * @param thunk operation to run to change version.
   */
  public void executeIn(final Runnable thunk) {
    for (;;) {
      Version initial;
      synchronized (this) {
        initial = getVersion();
      }
      Version.saveVersion(initial);
      try {
        thunk.run();
        if (moveFromVersionToCurrent(initial)) return;
      } finally {
        Version.restoreVersion();
      }
    }
  }
  
  /** Change the version from the current one
   * to the version constructed using it.
   * If the operation is not successful (the tracker
   * was changed since the version was initially read,
   * "false" is returned.  This method should be used
   * in a loop such as:
   * <pre>
   *   do {
   *     Version initial = tr.getVersion();
   *     Version.setVersion(initial);
   *     <em>make changes</em>
   *   } while (!tr.moveFromVersionToCurrent(initial));
   * </pre>
   * <p>
   * This idiom is implemented in {@link #executeIn(Runnable)}.
   * @see #executeIn(Runnable)
   */
  public synchronized boolean moveFromVersionToCurrent(Version initial) {
    if (getVersion() == initial) {
      setVersion(Version.getVersionLocal()); // or ... getVersion() ...
      return true;
    } else {
      return false;
    }
  }

  /** Return true if the current version is the same as
   * the one being tracked.
   */
  public abstract boolean isActive();

  /** Makes the current version equal to the one in the tracker.
   * @deprecated use Version.setVersion(this.getVersion())
   */
  @Deprecated
  public abstract void makeActive();

  public synchronized void addVersionTrackerListener( final VersionTrackerListener l )
  {
    listeners.add( l );
  }

  public synchronized void removeVersionTrackerListener( final VersionTrackerListener l )
  {
    listeners.remove( l );
  }

  @SuppressWarnings("unchecked")
  protected void fireVersionTrackerEvent( final Version v1, final Version v2 )
  {
    final VersionTrackerEvent vte = new VersionTrackerEvent( this, v1, v2 );
    Vector<VersionTrackerListener> l;
    synchronized( this ) {
      l = (Vector<VersionTrackerListener>) listeners.clone();
    }
    for( int i = 0; i < l.size(); i++ )
    {
      VersionTrackerListener vtl = l.elementAt( i );
      vtl.versionChanged( vte );
    }
  }
}
