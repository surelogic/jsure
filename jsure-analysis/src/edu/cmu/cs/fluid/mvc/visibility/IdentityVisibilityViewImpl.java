/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/IdentityVisibilityViewImpl.java,v 1.12 2007/07/05 18:15:23 aarong Exp $
 *
 * IdentityVisibilityViewImpl.java
 * Created on March 15, 2002, 4:07 PM
 */

package edu.cmu.cs.fluid.mvc.visibility;

import java.util.List;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ModelEvent;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.ir.*;

/**
 * Minimal implemenation of {@link IdentityVisibilityView}.
 *
 * <P>Supports the model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link VisibilityModel#VISIBILITY_OF}
 * </ul>
 *
 * <P>Support the node-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link VisibilityModel#IS_VISIBLE}
 * </ul>

 * @author Aaron Greenhouse
 */
final class IdentityVisibilityViewImpl
extends AbstractModelToVisibilityStatefulView
implements IdentityVisibilityView
{
  //===========================================================
  //== Constructor
  //===========================================================
 
  public IdentityVisibilityViewImpl(
    final String name, final Model src, final ModelCore.Factory mf,
    final ViewCore.Factory vf, final VisibilityModelCore.Factory vmf )
  throws SlotAlreadyRegisteredException
  {
    super( name, mf, vf, vmf, src );

    /* Fill in the value of the SRC_MODELS attribute */
    final IRSequence<Model> srcModels =
      ConstantSlotFactory.prototype.newSequence(1);
    srcModels.setElementAt( srcModel, 0 );
    viewCore.setSourceModels( srcModels );
        
    /* Our isVisible attribute is always true. */
    final SlotInfo<Boolean> isVisible = 
      ConstantSlotFactory.prototype.newAttribute(
        name + "-" + VisibilityModel.IS_VISIBLE, IRBooleanType.prototype,
        Boolean.TRUE );
    attrManager.addNodeAttribute(
      VisibilityModel.IS_VISIBLE, Model.STRUCTURAL, isVisible );
    visModCore.setIsVisibleAttribute( isVisible );
    
    /*
     * Model state is always up to date, don't need to explicitly
     * init model state with a call to rebuildModel();
     */

    /* Attach to view chain */
    srcModel.addModelListener( srcModelBreakageHandler );
    finalizeInitialization();
  }
  
  
  
  //===========================================================
  //== AbstractModelToModel methods
  //===========================================================
  
  // inherit javadoc
  @Override
  protected void rebuildModel( final List events )
  {
    /*
     * The model is always up-to-date because it simply passes through
     * attributes of the source model.   We do, however, have to "pass"
     * the breakage down the chain.
     */
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }
}
