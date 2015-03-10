// $Header:
// /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/AbstractStitchedForest.java,v
// 1.15 2003/07/15 18:39:10 thallora Exp $

package edu.cmu.cs.fluid.mvc.tree.stitch;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.tree.*;

/**
 * An abstract implementation of {@link ConfigurableForestView}. Provides the
 * minimal implementation of the interface, but is extensible. Building of the
 * exported model is abstracted into calls to {@link #setupNode(IRNode)}and
 * {@link #addSubtree(IRNode, IRNode)}to support the constraints of specialized Forests,
 * specifically SyntaxForests.
 * 
 * 
 * <p>
 * A non-abstract subclass must implement the {@link ConfigurableForestView}
 * interface.
 * 
 * @author Edwin Chan
 */
public abstract class AbstractStitchedForest
  extends AbstractForestToForestStatefulView {
  
  Object temp = DebugUnparser.class;
  
  //===========================================================
  //== Fields
  //===========================================================
  
  /**
   * Logger for this class
   */
  static final Logger LOG = SLLogger.getLogger("MV.tree.stitch");

  /** The source forest. */
  protected final ForestModel srcModel;

  /** The attribute inheritance policy. */
  private final AttributeInheritancePolicy attrPolicy;
  
  protected IStitchTreeTransform transform;

  //===========================================================
  //== Constructor
  //===========================================================

  protected AbstractStitchedForest(
    final String name,
    final ForestModel src,
    final ModelCore.Factory mf,
    final ViewCore.Factory vf,
    final ForestModelCore.Factory fmf,
    final AttributeInheritancePolicy aip)
    throws SlotAlreadyRegisteredException 
  {
    // Init model parts
    super(
      name,
      mf,
      vf,
      fmf,
      LocalAttributeManagerFactory.prototype,
      ProxySupportingAttributeManagerFactory.prototype);

    srcModel = src;
    attrPolicy = aip;

    inheritManager.inheritAttributesFromModel(
        srcModel,
        attrPolicy,
        AttributeChangedCallback.nullCallback);
    
    // Initialize Model-level attributes
    final IRSequence<Model> srcModels = ConstantSlotFactory.prototype.newSequence(1);
    srcModels.setElementAt(srcModel, 0);
    viewCore.setSourceModels(srcModels);

    // Connect model-view chain
    srcModel.addModelListener(srcModelBreakageHandler);
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Inner Classes
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Inner Classes
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Local State
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== Callbacks
  //===========================================================

  //===========================================================
  //== Rebuild methods
  //===========================================================

  /**
	 * This causes the source model to be traversed and the sub-model to be
	 * built.
	 */
  @Override
  protected final void rebuildModel(final List<ModelEvent> events)
    throws InterruptedException {
    LOG.info("Rebuilding " + this.getName());
    synchronized (structLock) {
      // Clear the existing tree model...?
      forestModCore.clearForest();
      clearNodes();
      
      // Build the new model
      final Iterator<IRNode> roots = srcModel.getRoots();
      while (roots.hasNext()) {
        if (rebuildWorker.isInterrupted())
          throw cannedInterrupt;
        
        final IRNode root = roots.next();
        transform.rewriteTree(root);
      }
      forestModCore.precomputeNodes();
    }

    LOG.info("Finished rebuilding CFV");

    // Break our views
    modelCore.fireModelEvent(new ModelEvent(this));
  }

  void clearNodes() {    
    //XXX
  }
  
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Local State
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
}
