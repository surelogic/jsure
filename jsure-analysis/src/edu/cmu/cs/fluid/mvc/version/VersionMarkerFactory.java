/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/VersionMarkerFactory.java,v 1.5 2003/07/15 18:39:11 thallora Exp $ */
package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.version.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * Factory for creating instances of {@link VersionMarkerModel}.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 */
public final class VersionMarkerFactory
implements VersionMarkerModel.Factory
{
  /**
   * The singleton reference.
   */
  public static final VersionMarkerModel.Factory prototype =
    new VersionMarkerFactory();
  
  
  
  /**
   * Use the singleton reference {@link #prototype}.
   */
  protected VersionMarkerFactory()
  {
  }

  
  
  /**
   * Create a new minimal implementation of VersionMarkerModel that
   * initially points given version.
   */
  @Override
  public VersionMarkerModel create( final String name, final Version version )
  throws SlotAlreadyRegisteredException
  {
    return new VersionMarkerImpl(
                 name, ModelCore.simpleFactory,
                 VersionTrackerModelCore.standardFactory, version );
  }  

}