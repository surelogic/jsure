/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedState.java,v 1.4 2008/06/24 19:13:12 thallora Exp $
 */
package edu.cmu.cs.fluid.version;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRChunk;
import edu.cmu.cs.fluid.ir.IRState;


/**
 * A state that can change in versioned space.
 * @author boyland
 */
public interface VersionedState extends IRState {

  static final Logger LOG = SLLogger.getLogger("IR.version");

  /**
   * Determine whether this state has a snapshot loaded for this version
   * @param v version for which snapshot is desired
   * @return true if a snapshot exists and is defined
   */
  public boolean snapshotIsDefined(Version v);

  /**
   * Determine whether this state has a delta loaded/defined for the entire era
   * up to the version mentioned, at least.
   * @param e era of delta
   * @param lastV last version of the era known at this time
   * @return true if a delta exista nd is defined to this version
   */
  public boolean deltaIsDefined(Era e, Version lastV);

  /**
   * Return true for shared versioned states: one with state that
   * must be loaded for the initial version.
   * @return true for a shared state.
   */
  public boolean isShared();

  public static class Operations extends IRState.Operations {
    public static VersionedState asVersionedState(final IRState st) {
      if (st instanceof VersionedState) return (VersionedState) st;
      if (st instanceof IRChunk) {
        return new VersionedState() {

          @Override
          public boolean snapshotIsDefined(Version v) {
            VersionedSnapshot vs = VersionedSnapshot.find((IRChunk)st,v);
            return vs != null && vs.isDefined();
          }

          @Override
          public boolean deltaIsDefined(Era e, Version lastV) {
            VersionedDelta vd = VersionedDelta.find((IRChunk)st,e);
            return vd != null && vd.isDefined(lastV);
          }

          @Override
          public boolean isShared() {
            // TODO Change if we add SharedRegion
            return ((IRChunk)st).getRegion() instanceof SharedVersionedRegion;
          }

          @Override
          public IRState getParent() {
            return st.getParent();
          }
          
        };
      }
      LOG.warning("Unknown versioned state " + st);
      return null;
    }  
  }
}