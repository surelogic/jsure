package edu.cmu.cs.fluid.mvc.version.tree;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.tree.ForestModel;
import edu.cmu.cs.fluid.mvc.version.VersionTrackerModel;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.version.Version;

/**
 *
 * @author Aaron Greenhouse
 */
final class FixedVersionForestProjectionImpl
extends AbstractFixedVersionForestProjection
implements FixedVersionForestProjection
{
  //===========================================================
  //== Constructor
  //===========================================================
  
  protected FixedVersionForestProjectionImpl(
    final String name, final ForestModel srcModel, final VersionTrackerModel vc,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final Version initVersion )
  throws SlotAlreadyRegisteredException
  {
    super( name, srcModel, vc, mf, vf, initVersion );
    finalizeInitialization();
  }
}
