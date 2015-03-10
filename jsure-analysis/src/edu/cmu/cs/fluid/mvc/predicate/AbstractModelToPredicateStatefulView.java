/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/AbstractModelToPredicateStatefulView.java,v 1.20 2006/03/29 19:54:51 chance Exp $*/
package edu.cmu.cs.fluid.mvc.predicate;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.sequence.AbstractModelToSequenceStatefulView;
import edu.cmu.cs.fluid.mvc.sequence.SequenceModelCore;
import edu.cmu.cs.fluid.ir.*;

/**
 * @author Aaron Greenhouse
 */
public abstract class AbstractModelToPredicateStatefulView
  extends AbstractModelToSequenceStatefulView {
  /** The PredicateModelCore delegate */
  protected final PredicateModelCore predModCore;

  /**
   * The source model; the model whose attributes this model
   * is modeling.
   */
  protected final Model srcModel;

  protected final Map<IRNode,IRNode> addedPredicates = new HashMap<IRNode,IRNode>();

  //===========================================================
  //== Constructor
  //===========================================================

  public AbstractModelToPredicateStatefulView(
    final String name,
    final Model src,
    final ModelCore.Factory mf,
    final ViewCore.Factory vf,
    final SequenceModelCore.Factory smf,
    final PredicateModelCore.Factory amf,
    final AttributeManager.Factory attrFactory,
    final AttributeInheritanceManager.Factory inheritFactory)
    throws SlotAlreadyRegisteredException {
    super(name, mf, vf, smf, attrFactory, inheritFactory);
    predModCore =
      amf.create(
        name,
        this,
        structLock,
        attrManager,
        new AttrAttrsChangedCallback());
    srcModel = src;

    // Initialize Model-level attributes
    predModCore.setPredicatesOf(srcModel);
    final IRSequence<Model> srcModels = ConstantSlotFactory.prototype.newSequence(1);
    srcModels.setElementAt(src, 0);
    viewCore.setSourceModels(srcModels);
  }

  //===========================================================
  //== Callback
  //===========================================================

  private class AttrAttrsChangedCallback
    extends AbstractAttributeChangedCallback {
    @Override
    protected void attributeChangedImpl(
      final String attr,
      final IRNode node,
      final Object val) {
      if ((attr == PredicateModel.IS_VISIBLE)
        || (attr == PredicateModel.IS_STYLED)) {
        modelCore.fireModelEvent(
          new AttributeValuesChangedEvent(
            AbstractModelToPredicateStatefulView.this,
            node,
            attr,
            val));
      }
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin PredicateModel portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== Attribute Convienence methods  
  //===========================================================

  // Inherit JavaDoc from PredicateModel
  public final IRNode getAttributeNode(final IRNode node) {
    synchronized (structLock) {
      return predModCore.getAttributeNode(node);
    }
  }

  // Inherit JavaDoc from PredicateModel
  public final AttributePredicate getPredicate(final IRNode node) {
    synchronized (structLock) {
      return predModCore.getPredicate(node);
    }
  }

  // Inherit JavaDoc from PredicateModel
  public final IREnumeratedType.Element isVisible(final IRNode node) {
    synchronized (structLock) {
      return predModCore.isVisible(node);
    }
  }

  // Inherit JavaDoc from PredicateModel
  public final void setVisible(
    final IRNode node,
    final IREnumeratedType.Element vis) {
    synchronized (structLock) {
      predModCore.setVisible(node, vis);
    }
  }

  // Inherit JavaDoc from PredicateModel
  public final boolean isStyled(final IRNode node) {
    synchronized (structLock) {
      return predModCore.isStyled(node);
    }
  }

  // Inherit JavaDoc from PredicateModel
  public final void setStyled(final IRNode node, final boolean sty) {
    synchronized (structLock) {
      predModCore.setStyled(node, sty);
    }
  }

  // Inherit JavaDoc from PredicateModel
  public final SlotInfo getAttribute(final IRNode node) {
    synchronized (structLock) {
      return predModCore.getAttribute(node);
    }
  }

  // Inherit JavaDoc from PredicateModel
  public final String getLabel(final IRNode node) {
    synchronized (structLock) {
      return predModCore.getLabel(node);
    }
  }

  // Inherit JavaDoc from PredicateModel
  public final void setLabel(final IRNode node, final String label) {
    synchronized (structLock) {
      predModCore.setLabel(node, label);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final String nodeValueToString(final IRNode node, final String attr)
    throws UnknownAttributeException {
    synchronized (structLock) {
      return predModCore.nodeValueToString(modelCore, srcModel, node, attr);
    }
  }

  /** Get the visibility Element corresponding to the String */
  public final IREnumeratedType.Element getEnumElt(final String name) {
    final IREnumeratedType type = PredicateModelCore.getVisibleEnum();
    final int idx = type.getIndex(name);
    return (idx == -1) ? null : type.getElement(idx);
  }

  //===========================================================
  //== Methods to customize the predicate view
  //===========================================================

  /** Set up required elements for the predicate */
  protected void initPredicate(
    IRNode node,
    IRNode attrNode,
    AttributePredicate pred,
    SlotInfo attribute) {
    predModCore.setAttributeNode(node, attrNode);
    predModCore.setPredicate(node, pred);
    predModCore.setAttribute(node, attribute);
  }

  /** */
  protected void configurePredicate(
    IRNode node,
    IREnumeratedType.Element visible,
    boolean styled) {
    predModCore.setVisible(node, visible);
    predModCore.setStyled(node, styled);
  }

  protected void appendPredicateNode(
    final IRNode node,
    final IRNode attrNode,
    final AttributePredicate p,
    final SlotInfo si) {
    seqModCore.appendElement(node);
    initPredicate(node, attrNode, p, si);
    // NOTE: Leave isVisible and isStyled alone
  }

  private IRNode getPredicateNode(IRNode attrNode) {
    IRNode n = addedPredicates.get(attrNode);
    if (n == null) {
      n = new MarkedIRNode("AbstractMtoPredView");
      addedPredicates.put(attrNode, n);
    }
    return n;
  }

  protected IRNode initPredicateNode(
    IRNode attrNode,
    AttributePredicate pred,
    IREnumeratedType.Element visible,
    boolean styled,
    SlotInfo attribute) {
    IRNode node = getPredicateNode(attrNode);
    initPredicate(node, attrNode, pred, attribute);
    configurePredicate(node, visible, styled);
    return node;
  }

  public IRNode addPredicateBefore(
    IRLocation loc,
    IRNode attrNode,
    AttributePredicate pred,
    IREnumeratedType.Element visible,
    boolean styled,
    SlotInfo attribute) {
    try {
      synchronized (structLock) {
        final IRNode node =
          initPredicateNode(attrNode, pred, visible, styled, attribute);
        seqModCore.insertElementBefore(node, loc);
        return node;
      }
    } finally {
      modelCore.fireModelEvent(new ModelEvent(this));
    }
  }

  public IRNode addPredicateAfter(
    IRLocation loc,
    IRNode attrNode,
    AttributePredicate pred,
    IREnumeratedType.Element visible,
    boolean styled,
    SlotInfo attribute) {
    try {
      synchronized (structLock) {
        final IRNode node =
          initPredicateNode(attrNode, pred, visible, styled, attribute);
        seqModCore.insertElementAfter(node, loc);
        return node;
      }
    } finally {
      modelCore.fireModelEvent(new ModelEvent(this));
    }
  }

  /** Added at the end */
  public IRNode addPredicate(
    IRNode attrNode,
    AttributePredicate pred,
    IREnumeratedType.Element visible,
    boolean styled,
    SlotInfo attribute) {
    try {
      synchronized (structLock) {
        final IRNode node =
          initPredicateNode(attrNode, pred, visible, styled, attribute);
        seqModCore.appendElement(node);
        return node;
      }
    } finally {
      modelCore.fireModelEvent(new ModelEvent(this));
    }
  }

  //===========================================================
  //== Pickled State methods
  //===========================================================

  /**
   * Get pickled representation of the model's current state.
   */
  public final PickledPredicateModelState getPickledState() {
    synchronized (structLock) {
      return predModCore.getPickledState();
    }
  }

  /**
   * State the state of the attribute model from a
   * pickled representation of the state.
   * Any attributes present in the pickle that are not currently present
   * in the model are ignored.  Any attributes in the model that are
   * not present in the pickle are moved to the end of the sequence,
   * with their relative order retained.
   * @exception IllegalArgumentException Thrown if the 
   * pickle did not come from this model.
   */
  public final void setStateFromPickle(final PickledPredicateModelState pickle) {
    synchronized (structLock) {
      predModCore.setStateFromPickle(pickle);
    }
    modelCore.fireModelEvent(new ModelEvent(this));
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End PredicateModel portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
}
