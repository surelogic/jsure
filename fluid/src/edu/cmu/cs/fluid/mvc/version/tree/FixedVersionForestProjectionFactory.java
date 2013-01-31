/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/tree/FixedVersionForestProjectionFactory.java,v 1.6 2003/07/15 18:39:11 thallora Exp $ */
package edu.cmu.cs.fluid.mvc.version.tree;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.tree.ForestModel;
import edu.cmu.cs.fluid.mvc.version.VersionTrackerModel;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Factory for creating instances of FixedVersionForestProjection.  Models returned
 * by the factory are stateful views that present their source model at a
 * fixed version.  The version is controlled by a separate VersionTracker
 * source model.  <em>The model assumes all attributes of the source model are
 * either immutable or versioned!</em>
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 */
public final class FixedVersionForestProjectionFactory
implements FixedVersionForestProjection.Factory
{
  /**
   * The singleton reference.
   */
  public static final FixedVersionForestProjectionFactory prototype =
    new FixedVersionForestProjectionFactory();
  
  
  
  /**
   * Use the singleton reference {@link #prototype}.
   */
  protected FixedVersionForestProjectionFactory()
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
  public final FixedVersionForestProjection create(
    final String name, final ForestModel srcModel,
    final VersionTrackerModel tracker )
  throws SlotAlreadyRegisteredException
  {
    return new FixedVersionForestProjectionImpl(
                 name, srcModel, tracker, ModelCore.simpleFactory,
                 ViewCore.standardFactory, tracker.getVersion() );
  }
}