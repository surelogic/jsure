/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/VersionSpaceFactory.java,v 1.5 2003/07/15 18:39:11 thallora Exp $ */
package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.version.*;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.tree.*;

/**
 * Factory for creating minimal instances of {@link VersionSpaceModel}.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 */
public final class VersionSpaceFactory
implements VersionSpaceModel.Factory
{
  /**
   * The singleton reference.
   */
  public static final VersionSpaceModel.Factory prototype =
    new VersionSpaceFactory();
  
  
  
  /**
   * Use the singleton reference {@link #prototype}.
   */
  protected VersionSpaceFactory()
  {
  }

  
  
  /**
   * Create a new minimal implementation of VersionMarkerModel that
   * initially points given version.
   */
  @Override
  public VersionSpaceModel create(
    final String name, final Version root, final String[] cursorNames )
  throws SlotAlreadyRegisteredException
  {
    return new VersionSpaceModelImpl(
                 name, root, cursorNames, ModelCore.simpleFactory,
                 new TreeForestModelCore.StandardFactory(
                       SimpleSlotFactory.prototype, false ),
                 VersionSpaceModelCore.standardFactory,
                 LocalAttributeManagerFactory.prototype );
  }  

}