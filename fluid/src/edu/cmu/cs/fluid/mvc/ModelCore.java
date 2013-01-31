// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/ModelCore.java,v 1.54 2007/01/12 18:53:29 chance Exp $

package edu.cmu.cs.fluid.mvc;

import java.util.*;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.Iteratable;

/**
 * Core implemenation of the <code>Model</code> interface.
 *
 * <p>Adds the model-level attributes {@link Model#MODEL_NODE} and
 * {@link Model#MODEL_NAME}.
 *
 * <p><em>No-longer creates the node-level attributes {@link Model#IS_ELLIPSIS},
 * {@link Model#ELLIDED_NODES}.  This is now left to the delegating model
 * to do.</eM>  Still handles the getter/setters for them though.  The model
 * should not add the attributes to the attribute manager itself.  Instead it
 * should invoke the the methods {@link #setIsEllipsisAttribute} and
 * {@link #setEllidedNodesAttribute} after the creating the attribute storage.
 *
 * @author Aaron Greenhouse
 */
public class ModelCore extends AbstractCore {
  /**
   * String representation of the undefined value.  Used by 
   * {@link #nodeValueToString} and {@link #compValueToString}
   */
  private static final String UNDEFINED_VALUE_STRING = "<undefined>";

  /**
   * String representation of the null value.  Used by 
   * {@link #nodeValueToString} and {@link #compValueToString}
   */
  private static final String NULL_VALUE_STRING = "<null>";

  /** List of model listeners */
  private ModelListener[] listeners;

  /** Value storage for the IS_ELLIPSIS and ELLIDED_NODES attributes */
  private SlotInfo<Boolean> isEllipsis;
  private SlotInfo<Set<IRNode>> ellidedNodes;

  /**
   * Flag indicating whether events should be buffered.  Evnets are 
   * buffered when this has a value greater than 0.  The value
   * indicates the level of nesting of atomic actions.
   */
  private volatile int bufferEvents = 0;

  /** Queue for storing buffered events. */
  private final List<ModelEvent> eventBuf = new ArrayList<ModelEvent>(10);

  //===========================================================
  //== Constructor
  //===========================================================

  /** 
   * Create a new model core object.
   */
  protected ModelCore(
    final String name,
    final Model model,
    final Object lock,
    final AttributeManager manager) {
    //=== Init fields
    //================
    super(model, lock, manager);
    listeners = new ModelListener[0];

    // these will be set later by setIsEllipsisAttribute and
    // setEllidedNodesAttribute.
    isEllipsis = null;
    ellidedNodes = null;

    //=== Init model attributes
    //==========================
    final ExplicitSlotFactory csf = ConstantExplicitSlotFactory.prototype;

    final IRNode node = new IndependentIRNode();
    final ComponentSlot<IRNode> nodeAttr =
      new SimpleComponentSlot<IRNode>(IRNodeType.prototype, csf, node);
    attrManager.addCompAttribute(Model.MODEL_NODE, Model.STRUCTURAL, nodeAttr);

    final ComponentSlot<String> nameAttr =
      new SimpleComponentSlot<String>(IRStringType.prototype, csf, name);
    attrManager.addCompAttribute(Model.MODEL_NAME, Model.STRUCTURAL, nameAttr);
  }

  //===========================================================
  //== Initialization helper functions
  //===========================================================

  /**
   * Set the core's reference to the attribute value storage for
   * the {@link Model#IS_ELLIPSIS} attribute.  This will cause 
   * the attribute to be registered with the attribute manager.
   * @exception NullPointerException Thrown if the provided value is null.
   * @exception IllegalStateException Thrown if the method has already been called.
   */
  public final void setIsEllipsisAttribute(final SlotInfo<Boolean> si) {
    if (si == null) {
      throw new NullPointerException("Provided storage is null.");
    }
    if (isEllipsis != null) {
      throw new IllegalStateException("Method cannot be called more than once.");
    }

    isEllipsis = si;
  }

  /**
   * Set the core's reference to the attribute value storage for
   * the {@link Model#ELLIDED_NODES} attribute.  This will cause 
   * the attribute to be registered with the attribute manager.
   * @exception NullPointerException Thrown if the provided value is null.
   * @exception IllegalStateException Thrown if the method has already been called.
   */
  public final void setEllidedNodesAttribute(final SlotInfo<Set<IRNode>> si) {
    if (si == null) {
      throw new NullPointerException("Provided storage is null.");
    }
    if (ellidedNodes != null) {
      throw new IllegalStateException("Method cannot be called more than once.");
    }

    ellidedNodes = si;
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Model Attribute methods
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== Node-to-attribute methods
  //===========================================================

  /**
   * Get the name of the attribute identified with the given IRNode.
   * Caller must hold the model's structural lock.
   * @param node The IRNode (representing an attribute) to query on.
   * @return An interned String giving the name of the attribute.
   * @exception UnknownAttributeException Thrown if the IRNode is not
   * identified with an attribute in this manager.
   */
  public String getAttributeName(final IRNode node)
    throws UnknownAttributeException {
    return attrManager.getAttributeName(node);
  }

  /**
   * Get the name-space of the attribute identified with the given IRNode.
   * Caller must hold the model's structural lock.
   * @param node The IRNode (representing an attribute) to query on.
   * @return <code>true</code> iff the attribute identified with given node
   *   is a node-level attribute.
   * @exception UnknownAttributeException Thrown if the IRNode is not
   * identified with an attribute in this manager.
   */
  public boolean isNodeAttribute(final IRNode node)
    throws UnknownAttributeException {
    return attrManager.isNodeAttribute(node);
  }

  //===========================================================
  //== Methods from Model Interface
  //===========================================================

  /**
   * Add a new user-defined model-level attribute.
   * Caller must hold the model's structural lock.
   * @param name The name of the attribute.
   * @param type The type of the attribute's value.
   * @param csf The factory to use to create the ComponentSlot that will store
   *            the attribute's value.
   * @param isMutable Flag indicating whether the attribute is mutable or not.
   * @param cb The Attribute Changed Callback to invoke when the attribute is
   *           changed (if the attribute is mutable).
   * @return The component slot that will also be returned by calls to
   *          {@link #getCompAttribute}; that is, it is appropriately 
   *          wrapped, etc.
   * @exception AttributeAlreadyExistsException Thrown if a model-level 
   * attribute of the same name already exists in the model.
   * @exception UnsupportedOperationException Thrown if the model is in a
   *   state in which new attributes cannot be created.  Currently this occurs
   *   if the model has any attached listeners (this may be removed in the
   *   future).
   */
  public <T> ComponentSlot<T> addCompAttribute(
    final String name,
    final IRType<T> type,
    final ComponentSlot.Factory csf,
    final boolean isMutable,
    final AttributeChangedCallback cb) {
    /* Atomic value assignment */
    final ModelListener[] mls = listeners;
    if (mls.length == 0) {
      final ComponentSlot<T> cs = csf.undefinedSlot(type);
      return attrManager.addCompAttribute(
        name,
        Model.USER_DEFINED,
        isMutable,
        cs,
        cb);
    } else {
      throw new UnsupportedOperationException("Cannot add user-defined attributes: the model has listeners.");
    }
  }

  /**
   * Get the names of the model-level attributes.
   * Caller must hold the model's structural lock.
   * @return An Iteratable over {@link java.lang.String}s.
   */
  public Iterator<String> getComponentAttributes() {
    return attrManager.getComponentAttributes();
  }

  /**
   * Query if a given model attribute is understood by the model.
   * Caller must hold the model's structural lock.
   */
  public boolean isComponentAttribute(final String att) {
    return attrManager.isComponentAttribute(att);
  }

  /**
   * Query if a given model attribute is mutable.
   * Caller must hold the model's structural lock.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public boolean isCompAttrMutable(final String att) {
    return attrManager.isCompAttrMutable(att);
  }

  /** 
   * Get the kind of a component attribute.
   * Caller must hold the model's structural lock.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public int getCompAttrKind(final String att) {
    return attrManager.getCompAttrKind(att);
  }

  /** 
   * Get the IRNode identified with a component attribute.
   * Caller must hold the model's structural lock.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public IRNode getCompAttrNode(final String att) {
    return attrManager.getCompAttrNode(att);
  }

  /**
   * Get the storage for a component-level attribute.
   * Caller must hold the model's structural lock.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public ComponentSlot getCompAttribute(final String att) {
    return attrManager.getCompAttribute(att);
  }

  /**
   * Default implementation that only checks to see if the
   * attributes are known.
   * @see Model#setCompAttributes
   */
  public void setCompAttributes(final AVPair[] pairs) {
    /* Delegate to an action because changing the values of the properties
     * causes model events to be generated.  By using an action, these
     * events are captured and fired out as one composite event once all the
     * property values have been changed.  This action doesn't have to fire
     * any other events by itself.
     */
    atomizeAction(new SetCompAttributes(pairs)).execute();
  }

  /**
   * Action used by {@link #setCompAttributes}
   */
  private class SetCompAttributes implements Model.AtomizedModelAction {
    private final AVPair[] pairs;

    public SetCompAttributes(final AVPair[] p) {
      pairs = p;
    }

    @Override
    public List<ModelEvent> execute() {
      final Model model = ModelCore.this.partOf;
      // Verify that all attributes exist and are mutable
      for (int i = 0; i < pairs.length; i++) {
        final String attr = pairs[i].getAttribute();
        if (!model.isComponentAttribute(attr)) {
          throw new UnknownAttributeException(attr, model);
        } else if (!model.isCompAttrMutable(attr)) {
          throw new IllegalArgumentException(
            "Attribute \"" + attr + "\" is immutable.");
        }
      }

      // Then set the values
      for (int i = 0; i < pairs.length; i++) {
        final ComponentSlot cs =
          model.getCompAttribute(pairs[i].getAttribute());
        cs.setValue(pairs[i].getValue());
      }
      return Collections.emptyList();
    }
  }

  //===========================================================
  //== New methods to manage the attributes
  //===========================================================

  /**
   * Convienence method for getting the value of the 
   * model attribute "Model.NODE".
   * Caller must hold the model's structural lock.
   */
  public IRNode getNode() {
    return (IRNode) getCompAttribute(Model.MODEL_NODE).getValue();
  }

  /**
   * Convienence method for getting the value of the 
   * model attribute "Model.NAME".
   * Caller must hold the model's structural lock.
   */
  public String getName() {
    return (String) getCompAttribute(Model.MODEL_NAME).getValue();
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Model Attribute methods
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Node Attribute methods
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  /**
   * Add a new user-defined node-level attribute.
   * Caller must hold the model's structural lock.
   * @param name The name of the attribute.
   * @param type The type of the attribute's value.
   * @param sf The SlotFactory to use to create the attributes storage.  
   * @param isMutable Flag indicating whether the attribute is mutable or not.
   * @param cb The Attribute Changed Callback to invoke when the attribute is
   *           changed (if the attribute is mutable).
   * @return The SltoInfo that will also be returned by calls to
   *          {@link #getNodeAttribute}; that is, it is appropriately 
   *          wrapped, etc.
   * @exception AttributeAlreadyExistsException Thrown if a node-level
   * attribute of the same name already exists in the model.
   * @exception UnsupportedOperationException Thrown if the model is in a
   *   state in which new attributes cannot be created.  Currently this occurs
   *   if the model has any attached listeners (this may be removed in the
   *   future).
   */
  public <T> SlotInfo<T> addNodeAttribute(
    final String name,
    final IRType<T> type,
    final SlotFactory sf,
    final boolean isMutable,
    final AttributeChangedCallback cb) {
    /* Atomic value assignment */
    final ModelListener[] mls = listeners;
    if (mls.length == 0) {
      try {
        final SlotInfo<T> si = sf.newAttribute(getName() + "-" + name, type);
        return attrManager.addNodeAttribute(
          name,
          Model.USER_DEFINED,
          isMutable,
          si,
          cb);
      } catch (final SlotAlreadyRegisteredException e) {
        // probably means the attribute already exists...
        throw new AttributeAlreadyExistsException(name, getName());
      }
    } else {
      throw new UnsupportedOperationException("Cannot add user-defined attributes: the model has listeners.");
    }
  }

  /**
   * Caller must hold the model's structural lock.
   */
  public <T> SlotInfo<T> addNodeAttribute(
    final String name,
    final IRType<T> type,
    final SlotFactory sf,
    final boolean isMutable,
    final Model[] srcs,
    final AttributeChangedCallback cb) {
    try {
      final SlotInfo<T> si = sf.newAttribute(getName() + "-" + name, type);
      return attrManager.addNodeAttribute(
        name,
        Model.USER_DEFINED,
        isMutable,
        srcs,
        si,
        cb);
    } catch (final SlotAlreadyRegisteredException e) {
      // probably means the attribute already exists...
      throw new AttributeAlreadyExistsException(name, getName());
    }
  }

  /**
   * Get the names of the node-level attributes.
   * Caller must hold the model's structural lock.
   * @return An Iteratable over {@link java.lang.String}s.
   */
  public Iterator<String> getNodeAttributes() {
    return attrManager.getNodeAttributes();
  }

  /**
   * Query if a given attribute is understood by the model.
   * Caller must hold the model's structural lock.
   */
  public boolean isNodeAttribute(final String att) {
    return attrManager.isNodeAttribute(att);
  }

  /**
   * Query if a given node attribute is mutable .
    * Caller must hold the model's structural lock.
  * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public boolean isNodeAttrMutable(final String attr) {
    return attrManager.isNodeAttrMutable(attr);
  }

  /**
   * Get the domain identified with a node attribute.
   * Caller must hold the model's structural lock.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public int getNodeAttrDomain(final String att) {
    return attrManager.getNodeAttrDomain(att);
  }

  /**
   * Get the source models whose union forms the domain identified with a
   * node attribute whose domain is {@link Model#SRC_DOMAIN}.
   * Caller must hold the model's structural lock.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   * @exception IllegalArgumentException if the given attribute does not 
   * have a domain of {@link Model#SRC_DOMAIN}.
   */
  public Model[] getNodeAttrDomainSrcs(final String att) {
    return attrManager.getNodeAttrDomainSrcs(att);
  }

  /**
   * Get the kind of a node attribute.
   * Caller must hold the model's structural lock.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public int getNodeAttrKind(final String att) {
    return attrManager.getNodeAttrKind(att);
  }

  /** 
   * Get the IRNode identified with a node attribute.
   * Caller must hold the model's structural lock.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public IRNode getNodeAttrNode(final String att) {
    return attrManager.getNodeAttrNode(att);
  }

  /**
   * Get the SlotInfo representing a node-level attribute.
   * Caller must hold the model's structural lock.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public SlotInfo getNodeAttribute(final String att) {
    return attrManager.getNodeAttribute(att);
  }

  /**
   * Default implementation that only checks to see if the
   * attributes are known and mutable.
   * Caller must hold the model's structural lock.
   * @see Model#setNodeAttributes
   */
  public void setNodeAttributes(final IRNode n, final AVPair[] pairs) {
    /* Delegate to an action because changing the values of the properties
     * causes model events to be generated.  By using an action, these
     * events are captured and fired out as one composite event once all the
     * property values have been changed.  This action doesn't have to fire
     * any other events by itself.
     */
    atomizeAction(new SetNodeAttributes(n, pairs)).execute();
  }

  /**
   * Action used by {@link #setNodeAttributes}
   */
  private class SetNodeAttributes implements Model.AtomizedModelAction {
    private final IRNode node;
    private final AVPair[] pairs;

    public SetNodeAttributes(final IRNode n, final AVPair[] p) {
      node = n;
      pairs = p;
    }

    @Override
    public List<ModelEvent> execute() {
      final Model model = ModelCore.this.partOf;
      // First very that all the attributes exist and are mutable
      for (int i = 0; i < pairs.length; i++) {
        final String attr = pairs[i].getAttribute();
        if (!model.isNodeAttribute(attr)) {
          throw new UnknownAttributeException(attr, model);
        } else if (!model.isNodeAttrMutable(attr)) {
          throw new IllegalArgumentException(
            "Attribute \"" + attr + "\" is immutable.");
        }
      }

      // Then set the values.
      for (int i = 0; i < pairs.length; i++) {
        final SlotInfo si = model.getNodeAttribute(pairs[i].getAttribute());
        node.setSlotValue(si, pairs[i].getValue());
      }
      return Collections.emptyList();
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Node Attribute methods
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== Attribute Convienence Methods
  //===========================================================

  /**
   * Get the value of the {@link Model#ELLIDED_NODES} attribute.
   * Caller must hold the model's structural lock.
   */
  @SuppressWarnings("unchecked")
  public Set<IRNode> getEllidedNodes(final IRNode node) {
    final Set<IRNode> val = node.getSlotValue(ellidedNodes);
    return val;
  }

  /**
   * Set the value of the {@link Model#ELLIDED_NODES} attribute.
   * Caller must hold the model's structural lock.
   */
  public void setEllidedNodes(final IRNode node, final Set<IRNode> nodes) {
    node.setSlotValue(ellidedNodes, nodes);
  }

  /**
   * Get the value of the {@link Model#IS_ELLIPSIS} attribute.
   * Caller must hold the model's structural lock.
   */
  public boolean isEllipsis(final IRNode node) {
    final Boolean val = node.getSlotValue(isEllipsis);
    return val.booleanValue();
  }

  /**
   * Set the value of the {@link Model#IS_ELLIPSIS} attribute.
   * Caller must hold the model's structural lock.
   */
  public void setEllipsis(final IRNode node, final boolean flag) {
    node.setSlotValue(isEllipsis, flag ? Boolean.TRUE : Boolean.FALSE);
  }

  //===========================================================
  //== Atomic Actions
  //===========================================================
  
  public Model.AtomizedModelAction atomizeAction(final Model.AtomizedModelAction action) {
    return new Model.AtomizedModelAction() {
      @Override
      public List<ModelEvent> execute() {
        synchronized (structLock) {
          List<ModelEvent> xtraEvents = null;
          try {
            bufferEvents += 1;
            xtraEvents = action.execute();
          } finally { // make sure buffering is turned off
            bufferEvents -= 1;
          }
  
          // Don't send an event if action is nested
          if (bufferEvents == 0) {
            // An event will not be sent if an exception was thrown above
            final List<ModelEvent> events = new ArrayList<ModelEvent>(eventBuf.size() + xtraEvents.size());
            events.addAll(eventBuf);
            for (ModelEvent evt : xtraEvents) {
              if (evt.getSourceAsModel() == partOf) events.add(evt);
            }
            eventBuf.clear();
            /* Don't send an event if no events were queued up during the
             * critical section
             */
            if (!events.isEmpty()) {
              fireModelEvent(new AggregateEvent(partOf, events));
            }
          }
          return xtraEvents;
        }
      }
    };
  }

  
  
  //===========================================================
  //== Model reflection methods
  //===========================================================

  /**
   * Return a string representation of a node.
   * This is model-specific, but it should approximate an
   * attribute list, givin string representations of the
   * all the attributes for which this node has a value.
   * Caller must hold the model's structural lock.
   * @exception IllegalArgumentException Thrown if <code>node</code>
   * is not part of the model.
   */
  public String toString(final IRNode node) {
    if (!partOf.isPresent(node)) {
      throw new IllegalArgumentException("Node " + node + " not in model.");
    } else {
      final Iterator nodeAttrs = attrManager.getNodeAttributes();
      final StringBuilder buf = new StringBuilder(partOf.idNode(node) + ": [");
      while (nodeAttrs.hasNext()) {
        final String attr = (String) nodeAttrs.next();
        final SlotInfo si = attrManager.getNodeAttribute(attr);
        if (node.valueExists(si)) {
          buf.append("[" + attr + " = " + nodeValueToString(node, attr) + "]");
        }
      }
      buf.append("]");
      return buf.toString();
    }
  }

  /**
   * Return a string identifying the given node.  This differs
   * from {@link #toString(IRNode)} in that it is only meant
   * to provide a name for a node, derived, for example, from
   * an attribute value.
   * Caller must hold the model's structural lock.
   */
  public String idNode(final IRNode node) {
    if (node == null)
      return NULL_VALUE_STRING;
    else
      return node.toString();
  }

  /**
   * Return a string representation of the value stored
   * in the given attribute for the given node.  This
   * implementation understands values of that store
   * IRNodes and Sequences of IRNodes,
   * and uses the method {@link #idNode} on their values.
   * Caller must hold the model's structural lock.
   * @exception UnknownAttributeException Thrown if the attribute
   * is not part of the model.
   */
  public String nodeValueToString(final IRNode node, final String attr)
    throws UnknownAttributeException {
    final SlotInfo si = attrManager.getNodeAttribute(attr);
    try {
      final Object value = node.getSlotValue(si);
      return processNodeValue(value, si.getType());
    } catch (final SlotUndefinedException e) {
      return UNDEFINED_VALUE_STRING;
    }
  }

  /**
   * Return a string representaiton of the value of a given
   * component-level attribute.  This
   * implementation understands values of that store
   * IRNodes and Sequences of IRNodes,
   * and uses the method {@link #idNode} on their values.
   * Caller must hold the model's structural lock.
   * @exception UnknownAttributeException Thrown if the attribute
   * is not part of the model.
   */
  public String compValueToString(final String attr)
    throws UnknownAttributeException {
    final ComponentSlot cs = attrManager.getCompAttribute(attr);
    try {
      final Object value = cs.getValue();
      return processCompValue(value, cs.getType());
    } catch (final SlotUndefinedException e) {
      return UNDEFINED_VALUE_STRING;
    }
  }

  /**
   * Caller must hold the model's structural lock.
   */
  private String processNodeValue(final Object value, final IRType type) {
    if (value == null) {
      return NULL_VALUE_STRING;
    } else {
      if (type instanceof IRNodeType) {
        final IRNode node = (IRNode) value;
        if (partOf.isPresent(node)) {
          return partOf.idNode(node);
        } else {
          return node.toString();
        }
      } else if (type instanceof IRSequenceType) {
        return processSequence((IRSequence) value, (IRSequenceType) type);
      }
    }
    return value.toString();
  }

  /**
   * Caller must hold the model's structural lock.
   */
  private String processCompValue(final Object value, final IRType type) {
    if (value == null) {
      return NULL_VALUE_STRING;
    } else {
      if (type instanceof IRNodeType) {
        final IRNode node = (IRNode) value;
        return node.toString();
      } else if (type instanceof IRSequenceType) {
        return processSequence((IRSequence) value, (IRSequenceType) type);
      }
    }
    return value.toString();
  }

  /*
   * Caller must hold the model's structural lock.
   */
  private String processSequence(
    final IRSequence seq,
    final IRSequenceType seqType) {
    if (seq == null) {
      return NULL_VALUE_STRING;
    } else if (seqType.getElementType() instanceof IRNodeType) {
      final StringBuilder buf = new StringBuilder("[");
      final Iteratable enm = seq.elements();
      while (enm.hasNext()) {
        // Watch out for undefined values in the sequence!
        try {
          final IRNode node = (IRNode) enm.next();
          if (partOf.isPresent(node))
            buf.append(partOf.idNode(node));
          else if (node != null)
            buf.append(node.toString());
          else
            buf.append(NULL_VALUE_STRING);
        } catch( SlotUndefinedException e ) {
          buf.append(UNDEFINED_VALUE_STRING);
        }
        if (enm.hasNext())
          buf.append(", ");
      }
      buf.append("]");
      return buf.toString();
    } else {
      return seq.toString();
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Model Listener Methods
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  /* Listeners are managed such that firing events is
   * cheap, but adding or removing listeners is 
   * expensive.
   */

  /**
   * Adds a listener that is notified whenever the
   * model is altered.
   */
  public void addModelListener(final ModelListener l) {
    synchronized (this) {
      final int len = listeners.length;

      for (int i = 0; i < len; i++) {
        if (l == listeners[i]) {
          throw new FluidError("Duplicated listener");
        }
      }

      final ModelListener[] new_list = new ModelListener[len + 1];
      System.arraycopy(listeners, 0, new_list, 0, len);
      new_list[len] = l;
      // Atomic value assignment!!!
      listeners = new_list;
    }
    // Inform the listener that it was added to a model
    l.addedToModel(this.partOf);
  }

  /**
   * Removes a model listener.
   */
  public void removeModelListener(final ModelListener l) {
    synchronized (this) {
      final int len = listeners.length;
      final ModelListener[] new_list = new ModelListener[len - 1];
      int where = -1;
      for (int i = 0;(where == -1) && (i < len); i++) {
        if (l == listeners[i])
          where = i;
      }

      if (where != -1) {
        System.arraycopy(listeners, 0, new_list, 0, where);
        System.arraycopy(listeners, where + 1, 
            new_list, where, len - where - 1);
        // Atomic value assignment!!!
        listeners = new_list;
      }
    }
    
    // Inform listener that it was removed
    l.removedFromModel(this.partOf);
  }

  /**
   * Remove all the listeners attached to the model.
   */
  public void removeAllModelListeners() {
    // clear out list of listeners
    final ModelListener[] copy;
    synchronized (this) { 
      copy = listeners;
      listeners = new ModelListener[0];
    }

    // inform listeners that they were removed
    for (int i = 0; i < copy.length; i++) {
      copy[i].removedFromModel(this.partOf);
      copy[i] = null;
    }
  }
  
  /**
   * Send a ModelEvent to all the listeners.
   */
  public void fireModelEvent(final ModelEvent e) {
    if (bufferEvents > 0) {
      // if we are buffering events the caller holds the struct lock
      eventBuf.add(e);
    } else {
      // Atomic value assignment!!!
      final ModelListener[] copy = listeners;
      for (int i = 0; i < copy.length; i++) {
        copy[i].breakView(e);
      }
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Model Listener Methods
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== Query about the relationship between models
  //===========================================================

  /**
   * Query if the model is above a view in a
   * model&ndash;view chain.
   */
  public boolean upChainFrom(final View v) {
    return v.downChainFrom(partOf);
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Factory classes 
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  public static interface Factory {
    public ModelCore create(
      String name,
      Model model,
      Object structLock,
      AttributeManager manager)
      throws SlotAlreadyRegisteredException;

    public SlotFactory getFactory();
  }

  public static final class StandardFactory implements Factory {
    final SlotFactory slotFactory;

    public StandardFactory(final SlotFactory sf) {
      slotFactory = sf;
    }

    @Override
    public final SlotFactory getFactory() {
      return slotFactory;
    }

    @Override
    public ModelCore create(
      final String name,
      final Model model,
      final Object structLock,
      final AttributeManager manager)
      throws SlotAlreadyRegisteredException {
      return new ModelCore(name, model, structLock, manager);
    }
  }

  /**
   * Protoptye factory for creating ModelCores that use SimpleSlots.
   * (This is a very common, though not exclusive, option.)
   */
  public static final ModelCore.Factory simpleFactory =
    new StandardFactory(SimpleSlotFactory.prototype);

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Factory classes 
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
}
