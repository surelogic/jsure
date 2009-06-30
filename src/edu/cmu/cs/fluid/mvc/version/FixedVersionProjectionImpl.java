package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.version.*;
import edu.cmu.cs.fluid.mvc.*;


/**
 * Stateful view that presents its source model at a fixed version.
 * The version is controlled by a separate VersionTracker source model.
 * <em>Assumes all attributes of the source model are either immutable or
 * versioned!</em>
 */
final class FixedVersionProjectionImpl
extends AbstractFixedVersionProjection
implements FixedVersionProjection
{
  //===========================================================
  //== Constructor
  //===========================================================

  /**
   * Create a new model projection that projects a versioned model into
   * an unversioned model by fixing a version.  The version at which the
   * model is projected may be changed at any time via the VersionTrackerModel
   * source model.  The attribute values of the model are also fixed at the 
   * same version, as well as the structure of any sequences in an attribute; 
   * <em>no other structured values are currently supported</em>.
   * @param name The name of the model.
   * @param srcModel The "Model" source model.
   * @param vc The "VersionTracker" source model.
   * @param mf The factory that creates the ModelCore object to use.
   * @param vf The factory that creates the ViewCore object to use.
   * @param initVersion The initial version at which to project the model.
   */
  protected FixedVersionProjectionImpl(
    final String name, final Model srcModel, final VersionTrackerModel vc,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final Version initVersion )
  throws SlotAlreadyRegisteredException
  {
    super( name, srcModel, vc, mf, vf, initVersion );
    finalizeInitialization();
  }
}
