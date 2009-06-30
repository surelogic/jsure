/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/VersionCursorModel.java,v 1.12 2003/07/15 21:47:18 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.ir.*;

/**
 * Empty specialization of {@link VersionSpaceToVersionTrackerStatefulView}.
 * Roughly analogous to the obsolete {@link edu.cmu.cs.fluid.version.VersionCursor}, which
 * operated at too low a level of abstraction.  This model replaces it and 
 * works at a higher level of abstraction by using {@link VersionSpaceModel}
 * instead of the version shadow tree directly.
 */
public interface VersionCursorModel
extends VersionSpaceToVersionTrackerStatefulView
{
  /**
   * Interface for factories that create instances of 
   * {@link VersionCursorModel}.
   */
  public static interface Factory
  {
    /**
     * Create a new VersionCursorModel that is constrained by the
     * given VersionSpace, and which initially points to the root
     * version of the space.
     */
    public VersionCursorModel create(
      String name, VersionSpaceModel src, boolean isFollowing )
    throws SlotAlreadyRegisteredException;
  }
}
