// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AbstractModel.java,v 1.36
// 2003/07/15 21:47:18 aarong Exp $

package edu.cmu.cs.fluid.mvc;

import java.util.Iterator;
import java.util.Set;

import edu.cmu.cs.fluid.ir.*;

/**
 * Abstract root class for implementing Models.
 * 
 * <p>
 * Delegates the creation of the {@link Model#IS_ELLIPSIS}and
 * {@link Model#ELLIDED_NODES}attributes to helper methods (see
 * {@link ModelCore}).
 * 
 * @author Aaron Greenhouse
 */
public abstract class AbstractModel extends DefaultDescribe implements Model {
  /** The attribute manager */
  protected final AttributeManager attrManager;

  /** The ModelCore delegate */
  protected final ModelCore modelCore;

  /** The structural lock */
  protected final Object structLock;

  /** The attribute changed callback used for user defined attributes. */
  protected final AttributeChangedCallback userDefinedCallback;

  //===========================================================
  //== Constructors
  //===========================================================

  /**
	 * Initialize the "generic" model portion of the model, excepting the
	 * {@link #IS_ELLIPSIS}and {@link #ELLIDED_NODES}attributes. <em>It is
	 * the responsibility of the subclass to both create the attributes and to
	 * invoke the methods {@link ModelCore#setIsEllipsisAttribute} and {@link
	 * ModelCore#setEllidedNodesAttribute} to set the
   * <code>ModelCore.isEllipsis</code> and <code>ModelCore.ellidedNodes</code>
   * fields.</em>
	 * 
	 * @param name
	 *          The name of the model.
	 * @param mf
	 *          The factory that creates the ModelCore object ot use.
	 * @param attrFactory
	 *          The factory that creates the AttributeManager object to use.
	 */
  protected AbstractModel(
    final String name,
    final ModelCore.Factory mf,
    final AttributeManager.Factory attrFactory)
    throws SlotAlreadyRegisteredException {
    structLock = new Object();

    // escape of 'this' is okay, because it is not dereferenced
    // in the called method
    attrManager = attrFactory.create(this, structLock);
    modelCore = mf.create(name, this, structLock, attrManager);
    userDefinedCallback = new UserDefinedCallback();
    
    /* XXX: It may be premature to do this here, but it is the only 
     * place I can put it that insures it always invoked for a model creation.
     */
    GlobalModelInformation.getInstance().newModel(this);
  }

  /**
	 * Initialize the "generic" model portion of the model, including the
	 * {@link #IS_ELLIPSIS}and {@link #ELLIDED_NODES}attributes.
	 * 
	 * @param name
	 *          The name of the model.
	 * @param mf
	 *          The factory that creates the ModelCore object ot use.
	 * @param attrFactory
	 *          The factory that creates the AttributeManager object to use.
	 * @param slotf
	 *          SlotFactory for creating the storage for the
	 *          <code>ModelCore.isEllipsis</code> and
   *          <code>ModelCore.ellidedNodes</code> fields.
	 */
  protected AbstractModel(
    final String name,
    final ModelCore.Factory mf,
    final AttributeManager.Factory attrFactory,
    final SlotFactory slotf)
    throws SlotAlreadyRegisteredException {
    this(name, mf, attrFactory);

    /*
		 * Create the IS_ELLIPSIS and ELLIDED_NODES attributes.
		 */
    final SlotInfo<Boolean> isEllipsis =
      slotf.newAttribute(
        name + "-" + Model.IS_ELLIPSIS,
        IRBooleanType.prototype,
        Boolean.FALSE);
    attrManager.addNodeAttribute(
      Model.IS_ELLIPSIS,
      Model.STRUCTURAL,
      isEllipsis);
    modelCore.setIsEllipsisAttribute(isEllipsis);

    /* XXX Bogus: Need a real ir type here */
    final SlotInfo<Set<IRNode>> ellidedNodes =
      slotf.newAttribute(
        name + "-" + Model.ELLIDED_NODES,
        nodeSetType);
    attrManager.addNodeAttribute(
      Model.ELLIDED_NODES,
      Model.STRUCTURAL,
      ellidedNodes);
    modelCore.setEllidedNodesAttribute(ellidedNodes);
  }

  private static final IRObjectType<Set<IRNode>> nodeSetType = new IRObjectType<Set<IRNode>>();
  
  /**
	 * This method must be invoked at the end of initialization of the model. Its
	 * job is to check that the model has been completely assembled. A <code>RuntimeException</code>
	 * is thrown if not.
	 * 
	 * <p>
	 * This implementation delegates to {@link #checkModelInitialization}, which
	 * checks that the {@link Model#IS_ELLIPSIS}and {@link Model#ELLIDED_NODES}
	 * attributes have been created.
	 */
  public void finalizeInitialization() {
    checkModelInitialization();
  }

  /**
	 * cHecks that the {@link Model#IS_ELLIPSIS}and {@link Model#ELLIDED_NODES}
	 * attributes have been created. A <code>RuntimeException</code> is thrown
	 * if not.
	 */
  protected final void checkModelInitialization() {
    try {
      /*
			 * See if the ellipsis and ellided nodes attributes exist. Trying to
			 * trigger the UnknownAttributeException caught below.
			 */
      attrManager.getNodeAttribute(Model.IS_ELLIPSIS);
      attrManager.getNodeAttribute(Model.ELLIDED_NODES);
    } catch (final UnknownAttributeException e) {
      throw new RuntimeException(
        "Required attribute \""
          + e.getAttribute()
          + "\" not present in model \""
          + getName()
          + "\"");
    }
  }

  /* (non-Javadoc)
   * By default, no parent
   * @see edu.cmu.cs.fluid.ir.IRState#getParent()
   */
  @Override
  public IRState getParent() {
    return null;
  }
  
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Inner Classes
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  /**
	 * Implements callbacks used when user-defined attributes (those added to the
	 * model using {@link #addCompAttribute}and {@link #addNodeAttribute}) are
	 * mutated. Model implementors should not have to create instances of this
	 * class (beyond the one used in AbstractModel). They should implement the
	 * method {@link AbstractModel#userDefinedAttributeChanged}instead.
	 */
  private class UserDefinedCallback extends AbstractAttributeChangedCallback {
    /**
		 * Simply invokes {@link AbstractModel#userDefinedAttributeChanged}.
		 */
    @Override
    protected void attributeChangedImpl(
      final String attr,
      final IRNode node,
      final Object value) {
      AbstractModel.this.userDefinedAttributeChanged(attr, node, value);
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Inner Classes
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== Model lifecycle methods
  //===========================================================

  /**
   * Basic implementation of the shutdown method.  Removes all
   * listeners registered with this class.
   */
  @Override
  public void shutdown() {
    // remove the listeners
    modelCore.removeAllModelListeners();
  }
  
  //===========================================================
  //== Handle Changes to User-Defined attributes
  //===========================================================

  /**
	 * Invoked by the Models user-defined attribute changed callback.
	 * Implementors should override this method as required. The default
	 * implementation causes an {@link AttributeValuesChangedEvent}to be sent.
	 * 
	 * @param attr
	 *          The name of the attribute; interned String.
	 * @param node
	 *          If the attribute is a node-level attribute, then this is the node
	 *          whose attribute value changed. If the attribute is a model-level
	 *          attribute then this is <code>null</code>.
	 * @param value
	 *          The new value of the attribute.
	 */
  protected void userDefinedAttributeChanged(
    final String attr,
    final IRNode node,
    final Object value) {
    AttributeValuesChangedEvent e = null;
    if (node == null) {
      e = new AttributeValuesChangedEvent(this, attr, value);
    } else {
      e = new AttributeValuesChangedEvent(this, node, attr, value);
    }
    modelCore.fireModelEvent(e);
  }

  //===========================================================
  //== Node-to-attribute methods
  //===========================================================

  /**
	 * Get the name of the attribute identified with the given IRNode.
	 * 
	 * @param node
	 *          The IRNode (representing an attribute) to query on.
	 * @return An interned String giving the name of the attribute.
	 * @exception UnknownAttributeException
	 *              Thrown if the IRNode is not identified with an attribute in
	 *              this manager.
	 */
  @Override
  public final String getAttributeName(final IRNode node)
    throws UnknownAttributeException {
    synchronized (structLock) {
      return modelCore.getAttributeName(node);
    }
  }

  /**
	 * Get the name-space of the attribute identified with the given IRNode.
	 * 
	 * @param node
	 *          The IRNode (representing an attribute) to query on.
	 * @return <code>true</code> iff the attribute identified with given node
	 *         is a node-level attribute.
	 * @exception UnknownAttributeException
	 *              Thrown if the IRNode is not identified with an attribute in
	 *              this manager.
	 */
  @Override
  public final boolean isNodeAttribute(final IRNode node)
    throws UnknownAttributeException {
    synchronized (structLock) {
      return modelCore.isNodeAttribute(node);
    }
  }

  //===========================================================
  //== Model attribute related methods
  //===========================================================

  // Inherit Javadoc from Model Interface
  @Override
  public <T> ComponentSlot<T> addCompAttribute(
    final String name,
    final IRType<T> type,
    final ComponentSlot.Factory csf,
    final boolean isMutable) {
    ComponentSlot<T> cs = null;
    synchronized (structLock) {
      cs =
        modelCore.addCompAttribute(
          name,
          type,
          csf,
          isMutable,
          userDefinedCallback);
    }
    modelCore.fireModelEvent(
      new AttributeAddedEvent(this, name, AttributeAddedEvent.MODEL_LEVEL));
    return cs;
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final Iterator<String> getComponentAttributes() {
    synchronized (structLock) {
      return modelCore.getComponentAttributes();
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final boolean isComponentAttribute(final String att) {
    synchronized (structLock) {
      return modelCore.isComponentAttribute(att);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final int getCompAttrKind(final String att) {
    synchronized (structLock) {
      return modelCore.getCompAttrKind(att);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final IRNode getCompAttrNode(final String att) {
    synchronized (structLock) {
      return modelCore.getCompAttrNode(att);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final boolean isCompAttrMutable(final String attr) {
    synchronized (structLock) {
      return modelCore.isCompAttrMutable(attr);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final ComponentSlot getCompAttribute(final String attr) {
    synchronized (structLock) {
      return modelCore.getCompAttribute(attr);
    }
  }

  /**
	 * Delegates to the default implementation in <code>ModelCore</code>.
	 * Override to use a different implemenation (to check constraints, for
	 * example).
	 * 
	 * @see ModelCore#setCompAttributes
	 */
  @Override
  public final void setCompAttributes(final AVPair[] pairs) {
    synchronized (structLock) {
      modelCore.setCompAttributes(pairs);
    }
  }

  //===========================================================
  //== Node attribute related methods
  //===========================================================

  // Inherit JavaDoc from Model interface
  @Override
  public final <T> SlotInfo<T> addNodeAttribute(
    final String name,
    final IRType<T> type,
    final SlotFactory sf,
    final boolean isMutable) {
    SlotInfo<T> si = null;
    synchronized (structLock) {
      si =
        modelCore.addNodeAttribute(
          name,
          type,
          sf,
          isMutable,
          userDefinedCallback);
    }
    modelCore.fireModelEvent(
      new AttributeAddedEvent(this, name, AttributeAddedEvent.NODE_LEVEL));
    return si;
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final <T> SlotInfo<T> addNodeAttribute(
    final String name,
    final IRType<T> type,
    final SlotFactory sf,
    final boolean isMutable,
    final Model[] srcs) {
    SlotInfo<T> si = null;
    synchronized (structLock) {
      si =
        modelCore.addNodeAttribute(
          name,
          type,
          sf,
          isMutable,
          srcs,
          userDefinedCallback);
    }
    modelCore.fireModelEvent(
      new AttributeAddedEvent(this, name, AttributeAddedEvent.NODE_LEVEL));
    return si;
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final Iterator<String> getNodeAttributes() {
    synchronized (structLock) {
      return modelCore.getNodeAttributes();
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final boolean isNodeAttribute(final String att) {
    synchronized (structLock) {
      return modelCore.isNodeAttribute(att);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final int getNodeAttrKind(final String attr) {
    synchronized (structLock) {
      return modelCore.getNodeAttrKind(attr);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final IRNode getNodeAttrNode(final String attr) {
    synchronized (structLock) {
      return modelCore.getNodeAttrNode(attr);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final boolean isNodeAttrMutable(final String attr) {
    synchronized (structLock) {
      return modelCore.isNodeAttrMutable(attr);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final int getNodeAttrDomain(final String att) {
    synchronized (structLock) {
      return modelCore.getNodeAttrDomain(att);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final Model[] getNodeAttrDomainSrcs(final String att) {
    synchronized (structLock) {
      return modelCore.getNodeAttrDomainSrcs(att);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final SlotInfo getNodeAttribute(final String att) {
    synchronized (structLock) {
      return modelCore.getNodeAttribute(att);
    }
  }

  /**
	 * Delegates to the default implementation in <code>ModelCore</code>.
	 * Override to use a different implemenation (to check constraints, for
	 * example).
	 * 
	 * @see ModelCore#setNodeAttributes
	 */
  @Override
  public void setNodeAttributes(final IRNode node, final AVPair[] pairs) {
    synchronized (structLock) {
      modelCore.setNodeAttributes(node, pairs);
    }
  }

  //===========================================================
  //== Node methods
  //===========================================================

  // Inherit JavaDoc from Model interface
  @Override
  public abstract Iterator<IRNode> getNodes();

  // Inherit JavaDoc from Model interface
  @Override
  public abstract void addNode(IRNode n, AVPair[] vals);

  // Inherit JavaDoc from Model interface
  @Override
  public abstract void removeNode(IRNode n);

  /**
	 * Query whether the given node may have a value for the given attribute in
	 * the model.
	 * 
	 * @return <code>true</code> if the node is capable of having a value for
	 *         the given attribute in the model (although that value may be
	 *         currently undefined}; <code>false</code> if the node does not
	 *         currently and cannot ever have a value for the given attribute in
	 *         the model.
	 */
  // Inherit JavaDoc from Model interface
  @Override
  public final boolean isAttributable(final IRNode node, final String attr) {
    synchronized (structLock) {
      final Model[] srcs = attrManager.getNodeAttrDomainSrcs(attr);
      boolean attributable = false;

      if (srcs == null) {
        attributable =
          this.isPresent(node) || this.isOtherwiseAttributable(node);

      } else {
        for (int i = 0; !attributable && (i < srcs.length); i++) {
          attributable =
            srcs[i].isPresent(node) || srcs[i].isOtherwiseAttributable(node);
        }
      }
      return attributable;
    }
  }

  // inherit javadoc
  // explicitly list for completeness...
  @Override
  public abstract boolean isPresent(IRNode node);

  /**
	 * Query if a node that is not part of the model (e.g., for which
	 * {@link #isPresent}is <code>false</code>) has attribute values stored
	 * in the model anyway. This is primary for supporting proxy nodes in
	 * configurable views, but may have other uses in the future.
	 * 
	 * <p>
	 * This default implementation always returns <code>false</code>.
	 */
  @Override
  public boolean isOtherwiseAttributable(final IRNode node) {
    return false;
  }

  //===========================================================
  //== Attribute Convienence Methods
  //===========================================================

  // Inherit JavaDoc from Model interface
  @Override
  public final IRNode getNode() {
    synchronized (structLock) {
      return modelCore.getNode();
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final String getName() {
    synchronized (structLock) {
      return modelCore.getName();
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final Set<IRNode> getEllidedNodes(final IRNode node) {
    synchronized (structLock) {
      return modelCore.getEllidedNodes(node);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final boolean isEllipsis(final IRNode node) {
    synchronized (structLock) {
      return modelCore.isEllipsis(node);
    }
  }

  //===========================================================
  //== Atomic Actions
  //===========================================================

  @Override
  public final Model.AtomizedModelAction atomizeAction(final Model.AtomizedModelAction action) {
    return modelCore.atomizeAction(action);
  }

  //===========================================================
  //== Model reflection methods
  //===========================================================

  // Inherit JavaDoc from Model interface
  @Override
  public String toString(final IRNode node) {
    synchronized (structLock) {
      return modelCore.toString(node);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public String idNode(final IRNode node) {
    synchronized (structLock) {
      return modelCore.idNode(node);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public String nodeValueToString(final IRNode node, final String attr)
    throws UnknownAttributeException {
    synchronized (structLock) {
      return modelCore.nodeValueToString(node, attr);
    }
  }

  // Inherit JavaDoc from Model interface
  @Override
  public String compValueToString(final String attr)
    throws UnknownAttributeException {
    synchronized (structLock) {
      return modelCore.compValueToString(attr);
    }
  }

  //===========================================================
  //== Model Listener Methods
  //===========================================================

  // Inherit JavaDoc from Model interface
  @Override
  public final void addModelListener(final ModelListener l) {
    modelCore.addModelListener(l);
  }

  // Inherit JavaDoc from Model interface
  @Override
  public final void removeModelListener(final ModelListener l) {
    modelCore.removeModelListener(l);
  }

  //===========================================================
  //== Query about the relationship between models
  //===========================================================

  // Inherit JavaDoc from Model interface
  @Override
  public final boolean upChainFrom(final View v) {
    synchronized (structLock) {
      return modelCore.upChainFrom(v);
    }
  }
  
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Model portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
}
