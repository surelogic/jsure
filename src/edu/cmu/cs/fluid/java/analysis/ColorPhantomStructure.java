/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/ColorPhantomStructure.java,v 1.3 2007/07/09 14:08:29 chance Exp $*/
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PhantomDrop;


/**
 * Phantom drop to represent the fact that the ColorStaticStructure for a CU
 * really does depend on the shape of that CU. We override DeponentInvalidAction
 * to make sure that we nuke the CU's entry in the ColorStaticCU static map,
 * and further that the map from IRNode to method for the ColorStaticCU being
 * invalided also gets invalidated.
 * @author dfsuther
 */
@Deprecated
public class ColorPhantomStructure extends PhantomDrop {

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    // remove all the classes and methods from the static structure for the world.
    // they'll get rebuilt in a subsequent pass if need be.
    ColorStaticCU.invalidateAction(getNode());
    super.deponentInvalidAction(invalidDeponent);
  }
  
  public ColorPhantomStructure(final IRNode node) {
    setNodeAndCompilationUnitDependency(node);
  }

}
