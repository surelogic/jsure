/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorPhantomStructure.java,v 1.3 2007/07/09 13:39:26 chance Exp $*/
package com.surelogic.analysis.threadroles;

import com.surelogic.RequiresLock;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.IThreadRoleDrop;


/**
 * Phantom drop to represent the fact that the ColorStaticStructure for a CU
 * really does depend on the shape of that CU. We override DeponentInvalidAction
 * to make sure that we nuke the CU's entry in the ColorStaticCU static map,
 * and further that the map from IRNode to method for the ColorStaticCU being
 * invalided also gets invalidated.
 * @author dfsuther
 */
public class TRolePhantomStructure extends IRReferenceDrop implements IThreadRoleDrop {

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  @RequiresLock("SeaLock")
  protected void deponentInvalidAction(Drop invalidDeponent) {
    // remove all the classes and methods from the static structure for the world.
    // they'll get rebuilt in a subsequent pass if need be.
    TRoleStaticCU.invalidateAction(getNode());
    super.deponentInvalidAction(invalidDeponent);
  }
  
  public TRolePhantomStructure(final IRNode node) {
    super(node);
  }

}
