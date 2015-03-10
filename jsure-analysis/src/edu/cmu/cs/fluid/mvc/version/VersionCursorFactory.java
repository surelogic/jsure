/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/VersionCursorFactory.java,v 1.5 2003/07/15 18:39:11 thallora Exp $ */
package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * Factory for creating instances of {@link VersionCursorModel}.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 */
public final class VersionCursorFactory
implements VersionCursorModel.Factory
{
  /**
   * The singleton reference.
   */
  public static final VersionCursorFactory prototype =
    new VersionCursorFactory();
  
  
  
  /**
   * Use the singleton reference {@link #prototype}.
   */
  protected VersionCursorFactory()
  {
  }

  
  
  /**
   * Create a new VersionCursorModel that is constrained by the
   * given VersionSpace, and which initially points to the root
   * version of the space.
   */
  @Override
  public VersionCursorModel create(
    final String name, final VersionSpaceModel src, final boolean isFollowing )
  throws SlotAlreadyRegisteredException
  {
    return new VersionCursorImpl(
                 name, src, ModelCore.simpleFactory,
                 ViewCore.standardFactory, VersionTrackerModelCore.standardFactory,
                 VersionSpaceToVersionTrackerStatefulViewCore.standardFactory,
                 isFollowing );
  }  
}