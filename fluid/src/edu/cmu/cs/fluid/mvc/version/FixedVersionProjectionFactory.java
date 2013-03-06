/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/FixedVersionProjectionFactory.java,v 1.5 2003/07/15 18:39:11 thallora Exp $ */
package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * Factory for creating instances of FixedVersionProjection.  Models returned
 * by the factory are stateful views that present their source model at a
 * fixed version.  The version is controlled by a separate VersionTracker
 * source model.  <em>The model assumes all attributes of the source model are
 * either immutable or versioned!</em>
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 */
public final class FixedVersionProjectionFactory
implements FixedVersionProjection.Factory
{
  /**
   * The singleton reference.
   */
  public static final FixedVersionProjectionFactory prototype =
    new FixedVersionProjectionFactory();
  
  
  
  /**
   * Use the singleton reference {@link #prototype}.
   */
  protected FixedVersionProjectionFactory()
  {
  }
  

  
  /**
   * Create a new fixed-version projection.  
   * @param name The name to give to the new model.
   * @param srcModel the versioned model to create a projection of.
   * @param tracker The version-tracker model that will control the version
   *               at which <code>srcModel</code> is projected.
   */
  @Override
  public final FixedVersionProjection create(
    final String name, final Model srcModel, final VersionTrackerModel tracker )
  throws SlotAlreadyRegisteredException
  {
    return new FixedVersionProjectionImpl(
                 name, srcModel, tracker, ModelCore.simpleFactory,
                 ViewCore.standardFactory, tracker.getVersion() );
  }
}