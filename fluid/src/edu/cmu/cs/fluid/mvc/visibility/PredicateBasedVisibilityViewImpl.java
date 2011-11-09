/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/PredicateBasedVisibilityViewImpl.java,v 1.7 2003/07/15 18:39:10 thallora Exp $
 *
 * PredicateBasedVisibilityViewImpl.java
 * Created on March 18, 2002, 4:49 PM
 */

package edu.cmu.cs.fluid.mvc.visibility;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * A minimal implemenation of {@link PredicateBasedVisibilityView}.
 *
 * @author Aaron Greenhouse
 */
final class PredicateBasedVisibilityViewImpl
extends AbstractPredicateBasedVisibilityView
{
  //===========================================================
  //== Constructor
  //===========================================================

  public PredicateBasedVisibilityViewImpl(
    final String name, final Model srcModel, final PredicateModel predModel,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final VisibilityModelCore.Factory vmf )
  throws SlotAlreadyRegisteredException
  {
    super( name, srcModel, predModel, mf, vf, vmf );
    
    /* init model state */
    rebuildModel();
    
    /* 
     * Add listeners:
     *  - To the source model in case new nodes are added, etc.
     *  - To the predicate model in case the rules are changed, etc.
     */
    srcModel.addModelListener( srcModelBreakageHandler );
    predModel.addModelListener( srcModelBreakageHandler );
    finalizeInitialization();
  }
}
