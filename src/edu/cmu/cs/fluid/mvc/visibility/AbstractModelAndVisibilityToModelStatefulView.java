/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/AbstractModelAndVisibilityToModelStatefulView.java,v 1.16 2006/03/30 16:20:26 chance Exp $ */
package edu.cmu.cs.fluid.mvc.visibility;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.ir.ConstantSlotFactory;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;


public abstract class AbstractModelAndVisibilityToModelStatefulView
extends AbstractModelToModelStatefulView
implements ModelAndVisibilityToModelStatefulView
{
  /** The source model */
  protected final Model srcModel;

  /** The source visibility model. */
  protected final VisibilityModel srcVisModel;



  //===========================================================
  //== Constructor
  //===========================================================

  public AbstractModelAndVisibilityToModelStatefulView(
    final String name, final Model srcModel, final VisibilityModel srcVisModel,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final AttributeManager.Factory attrFactory,
    final AttributeInheritanceManager.Factory inheritFactory  )
  throws SlotAlreadyRegisteredException
  {
    super( name, mf, vf, mf.getFactory(), attrFactory, inheritFactory );
    this.srcModel = srcModel;
    this.srcVisModel = srcVisModel;

    // Check for consistency
    final Model m =
      (Model)srcVisModel.getCompAttribute( VisibilityModel.VISIBILITY_OF ).getValue();
    if( m != srcModel ) {
      throw new IllegalArgumentException(
        "Source Model \"" + srcModel.getName() +
        "\" is not the source for Source Visibility Model \"" +
        srcVisModel.getName() + "\"" );
    }

    // Initialize Model-level attributes
    final IRSequence<Model> srcModels = ConstantSlotFactory.prototype.newSequence(2);
    srcModels.setElementAt( srcModel, 0 );
    srcModels.setElementAt( srcVisModel, 1 );
    viewCore.setSourceModels( srcModels );
  }
}

