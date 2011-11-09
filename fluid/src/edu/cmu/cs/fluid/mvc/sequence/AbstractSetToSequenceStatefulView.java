/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/AbstractSetToSequenceStatefulView.java,v 1.12 2006/03/30 16:20:26 chance Exp $ */
package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.mvc.AttributeInheritanceManager;
import edu.cmu.cs.fluid.mvc.AttributeManager;
import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.set.SetModel;
import edu.cmu.cs.fluid.ir.ConstantSlotFactory;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * <em>Note</em>: Initializes the SRC_MODELS attribute.
 *
 * @author Aaron Greenhouse
 */
public abstract class AbstractSetToSequenceStatefulView
extends AbstractModelToSequenceStatefulView
implements SetToSequenceStatefulView
{
  /** The set model being viewed. */
  protected final SetModel srcModel;

  
  
  //===========================================================
  //== Constructor
  //===========================================================

  /**
   * Create a new SetToSequenceStatefulView.
   * @param name The name of the stateful view
   * @param mf The factory to use to create the model core
   * @param vf The factory to use to create the view core
   * @param smf The factory to use to create the sequence model core.
   */
  public AbstractSetToSequenceStatefulView(
    final String name, final SetModel src,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final SequenceModelCore.Factory smf, 
    final AttributeManager.Factory attrFactory,
    final AttributeInheritanceManager.Factory inheritFactory )
  throws SlotAlreadyRegisteredException
  {
    super( name, mf, vf, smf, attrFactory, inheritFactory );
    srcModel = src;

    // Initialize Model-level attributes
    final IRSequence<Model> srcModels = ConstantSlotFactory.prototype.newSequence( 1 );
    srcModels.setElementAt( src, 0 );
    viewCore.setSourceModels( srcModels );
  }
}
