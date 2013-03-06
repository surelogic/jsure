/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/diff/DifferenceModelCore.java,v 1.12 2006/03/30 19:47:21 chance Exp $ */
package edu.cmu.cs.fluid.mvc.diff;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.ir.*;

/**
 * Core implementation of the <code>DifferenceModel</code> interface.
 * <p>Adds the model-level attributes {@link DifferenceModel#BASE_MODEL}
 * and {@link DifferenceModel#DELTA_MODEL}.
 *
 * @author Aaron Greenhouse
 */
public class DifferenceModelCore
extends AbstractCore
{
  //===========================================================
  //== Fields
  //===========================================================

  /** Storage for the {@link DifferenceModel#BASE_MODEL} attribute. */
  private final ComponentSlot<Model> baseModelAttr;
  
  /** Storage for the {@link DifferenceModel#DELTA_MODEL} attribute. */
  private final ComponentSlot<Model> deltaModelAttr;



  //===========================================================
  //== Constructor
  //===========================================================

  protected DifferenceModelCore(
    final String name, final Model model, final Object lock,
    final AttributeManager manager )
  throws SlotAlreadyRegisteredException
  {
    super( model, lock, manager );

    // Init model attributes
    final ExplicitSlotFactory csf = ConstantExplicitSlotFactory.prototype;
    baseModelAttr = new SimpleComponentSlot<Model>( ModelType.prototype, csf );
    attrManager.addCompAttribute(
      DifferenceModel.BASE_MODEL, Model.STRUCTURAL, baseModelAttr );
    deltaModelAttr = new SimpleComponentSlot<Model>( ModelType.prototype, csf );
    attrManager.addCompAttribute(
      DifferenceModel.DELTA_MODEL, Model.STRUCTURAL, deltaModelAttr );
  }

  public void setSourceModels(
    final ViewCore viewCore, final Model baseModel, final Model deltaModel )
  {
    // Initialize Model-level attributes
    final IRSequence<Model> srcModels =
      ConstantSlotFactory.prototype.newSequence(2);
    srcModels.setElementAt( baseModel, 0 );
    srcModels.setElementAt( deltaModel, 1 );

    viewCore.setSourceModels( srcModels );
    baseModelAttr.setValue( baseModel );
    deltaModelAttr.setValue( deltaModel );
  }



  //===========================================================
  //== Convienence Methods
  //===========================================================

  public Model getBaseModel()
  {
    return baseModelAttr.getValue();
  }

  public Model getDeltaModel()
  {
    return deltaModelAttr.getValue();
  }



  //===========================================================
  //== ViewCore Factory Interfaces/Classes
  //===========================================================

  public static interface Factory 
  {
    public DifferenceModelCore create(
      String name, Model model, Object structLock,
      AttributeManager manager )
    throws SlotAlreadyRegisteredException;
  }
  
  private static class StandardFactory
  implements Factory
  {
    @Override
    public DifferenceModelCore create(
      final String name, final Model model, final Object structLock,
      final AttributeManager manager )
    throws SlotAlreadyRegisteredException
    {
      return new DifferenceModelCore( name, model, structLock, manager );
    }
  }

  public static final Factory standardFactory = new StandardFactory();
}

