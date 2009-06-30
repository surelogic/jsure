package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

public abstract class AbstractModelAndVersionTrackerToModelStatefulView
extends AbstractModelToModelStatefulView
{
  /** The source model */
  protected final Model srcModel;

  /** The source version cursor model. */
  protected final VersionTrackerModel tracker;



  //===========================================================
  //== Constructor
  //===========================================================

  /**
   * Initialize the "generic" model portion of the model, excepting the
   * {@link Model#IS_ELLIPSIS} and {@link Model#ELLIDED_NODES} attributes, and the 
   * VersionTracker portion of the model.  <em>It is
   * the responsibility of the subclass to both create the attributes and to
   * invoke the methods {@link ModelCore#setIsEllipsisAttribute} and {@link
   * ModelCore#setEllidedNodesAttribute} to set the
   * {@link ModelCore#isEllipsis} and {@link ModelCore#ellidedNodes} fields.</em>
   *
   * @param name The name of the model.
   * @param srcModel The "Model" source model.
   * @param vt The "VersionTracker" source model.
   * @param mf The factory that creates the ModelCore object to use.
   * @param vf The factory that creates the ViewCore object to use.
   * @param attrFactory The factory that creates the AttributeManager
   *                     object to use.
   * @param inheritFactory The factory that creates the attribute inheritance
   *                       manager to use.
   */
  public AbstractModelAndVersionTrackerToModelStatefulView(
    final String name, final Model srcModel, final VersionTrackerModel vt,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final AttributeManager.Factory attrFactory,
    final AttributeInheritanceManager.Factory inheritFactory )
  throws SlotAlreadyRegisteredException
  {
    /*
     * Don't have the super class create the IS_ELLIPSIS and ELLIDED_NODES
     * attributes.  It will be the responsibility of the subclass to 
     * create this.  In general, this should be done by inheriting them,
     * which is usually not done.
     */
    super( name, mf, vf, attrFactory, inheritFactory );
    this.srcModel = srcModel;
    this.tracker = vt;

    // Initialize Model-level attributes
    final IRSequence<Model> srcModels = ConstantSlotFactory.prototype.newSequence( 2 );
    srcModels.setElementAt( srcModel, 0 );
    srcModels.setElementAt( tracker, 1 );
    viewCore.setSourceModels( srcModels );
  }
}
