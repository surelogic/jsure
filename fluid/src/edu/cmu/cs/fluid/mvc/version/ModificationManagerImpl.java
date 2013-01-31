package edu.cmu.cs.fluid.mvc.version;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.version.Version;

/**
 * Default implementation of {@link ModificationManager}.
 */
final class ModificationManagerImpl implements ModificationManager {
  public static final Logger LOG = SLLogger.getLogger("MV.version");

  private final VersionSpaceModel versionSpace;
  private final VersionTrackerModel marker;

  private AtomicInteger reentranceCount = new AtomicInteger(0);

  public ModificationManagerImpl(
    final VersionSpaceModel vs,
    final VersionTrackerModel vm) {
    versionSpace = vs;
    marker = vm;
  }

  @Override
  public boolean isExecuting() {
	return reentranceCount.get() > 0;    
  }
  
  @Override
  public synchronized void executeAtomically(final Runnable runner) {
    final Version startVersion = marker.getVersion();
    Version endVersion = null;
    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("Current version is " + Version.getVersion());
      LOG.finer("Starting Runnable at version " + startVersion);
    }
    /*
		 * Avoid fiddling with the version if we are in a nested call to
		 * executeAtomically because rolling back the version after the nested call
		 * would cause a fork in the version space, which is probably not what the
		 * caller intends.
		 */
    final int newCount = reentranceCount.incrementAndGet();
    if (newCount == 1) {
      /*
			 * We aren't in a nested call, so save the current version and move the
			 * version to the one our version marker points to.
			 */
      Version.saveVersion(startVersion);
    }
    try {
      runner.run();
    } catch (final Exception e) {
      LOG.log(Level.SEVERE, "Exception from Runnable: "+e.getMessage(), e);
    } finally {
      endVersion = Version.getVersion();
      if (LOG.isLoggable(Level.FINER)) {
        LOG.finer("Runnable finished at version " + endVersion);
      }
      final int endCount = reentranceCount.decrementAndGet();
      if (endCount == 0)
        Version.restoreVersion();
    }

    /*
		 * If the version changed as a result of the Runnable we update the version
		 * space and the version marker.
		 */
    final Version currVersion = marker.getVersion();
    if (!endVersion.equals(startVersion) && !endVersion.equals(currVersion)) {
      // Changed so update the VersionSpace
      boolean debug = LOG.isLoggable(Level.FINER);

      if (debug) {
        LOG.finer(
          "Updating the version space from "
            + startVersion
            + " to "
            + endVersion);
      }
      versionSpace.addVersionNode(startVersion, endVersion);
      if (debug) {
        LOG.finer("Updating the version marker to " + endVersion);
      }
      marker.setVersion(endVersion);
    }
  }

  /*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.fluid.mvc.version.ModificationManager#executeAtMarker(java.lang.Runnable)
	 */
  @Override
  public void executeAtMarker(Runnable runner) {
	// TODO fix when VICs are used
    // FIX executeAtomically(runner);     
	final Version startVersion = marker.getVersion(); 
	Version.saveVersion( startVersion ); 
	try { 
		runner.run(); 
    } catch (final Exception e) {
        LOG.log(Level.SEVERE, "Exception from Runnable: "+e.getMessage(), e);
	} finally { 
		final Version endVersion = Version.getVersion(); 
		if (!endVersion.equals(startVersion)) {
			LOG.severe("Runner changed the version: "+runner); 
		}
		Version.restoreVersion(); 
	}		 
  }

  @Override
  public VersionTrackerModel getMarker() {
    return marker;
  }

  @Override
  public VersionSpaceModel getVersionSpace() {
    return versionSpace;
  }
}
