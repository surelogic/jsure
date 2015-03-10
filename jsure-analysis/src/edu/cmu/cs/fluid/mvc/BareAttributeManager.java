// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/BareAttributeManager.java,v 1.12 2007/07/10 22:16:30 aarong Exp $
package edu.cmu.cs.fluid.mvc;

import java.util.*;

import edu.cmu.cs.fluid.mvc.attributes.GuardedImmutableModelAttribute;
import edu.cmu.cs.fluid.mvc.attributes.GuardedImmutableNodeAttribute;
import edu.cmu.cs.fluid.mvc.attributes.GuardedMutableModelAttribute;
import edu.cmu.cs.fluid.mvc.attributes.GuardedNodeAttribute;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IndependentIRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * Bare bones implementation of the fundamental functionality of an attribute
 * manager.  The missing/abstracted functionality is that part which deals
 * with wrapping the underlying ComponentSlots and SlotInfos to make them
 * safe for exposure to Views of the model.
 *
 * <p>It is expected that attribute manager factories will be used to
 * shield the creation of LocalAttributeBuilders from models.  See
 * {@link edu.cmu.cs.fluid.mvc.LocalAttributeManagerFactory} for example.
 *
 * @author Aaron Greenhouse
 */

@SuppressWarnings("unchecked")
public class BareAttributeManager
  extends ModelPart
  implements AttributeManager {
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Fields 
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Maps for storing the attributes
  //===========================================================

  /**
   * Map from model attribute names to model attribute data record.
   */
  private final Map<String,CompAttrRecord> compAttrs;

  /**
   * Map from attribute name to node attribute data record.
   */
  private final Map<String,NodeAttrRecord> nodeAttrs;

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End fields
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Inner classes
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Classes for keeping information about attributes
  //===========================================================

  /**
   * Root abstract class for storing information about attributes.
   */
  private static abstract class AttributeRecord {
    /** The IRNode identifying the attribute (and the model it comes from). */
    public final IRNode node;

    /** The kind of the attribute */
    public final int kind;

    /** Whether <em>clients</em> of the model can modify the attribute. */
    public final boolean isMutable;

    /**
     * Initialize the record.
     */
    protected AttributeRecord(final int kind, final boolean isMutable) {
      this.node = new IndependentIRNode();
      this.kind = kind;
      this.isMutable = isMutable;
    }
  }

  /**
   * Abstract record for information about a
   * component-level attribute.
   */
  private static final class CompAttrRecord extends AttributeRecord {
    /**
     * The wrapped slot attribute that is presented 
     * to clients of the model.
     */
    public final ComponentSlot wrapped;

    /**
     * Creates a new <code>ModelAttrRecord</code> instance.
     *
     * @param kind The kind of the attribute.
     * @param mutable Whether the attribute should be mutable by
     * clients of the model.
     * @param ca The ComponentSLot that should be used by "the outside
     * world" to access the attribute.
     */
    public CompAttrRecord(
      final int kind,
      final boolean mutable,
      final ComponentSlot ca) {
      super(kind, mutable);
      this.wrapped = ca;
    }
  }

  /** 
   * Record for keeping track of information about a node-level attribute.
   */
  private static final class NodeAttrRecord extends AttributeRecord {
    /**
     * The list of source models whose union forms a superset of the
     * domain of this attribute; <code>null</code> if the domain is the
     * model itself.
     */
    public final Model[] sources;

    /** The SlotInfo used by the clients */
	public final SlotInfo wrappedSlotInfo;

    /**
     * Creates a new <code>NodeAttrRecord</code> instance.
     *
     * @param kind The kind of the attribute.
     * @param mutable Whehter the attribute should be modifiable by
     * clients of the model.
     * @param si The SlotInfo that should be used by "the outside world"
     * to access the attribute.
     */
    public NodeAttrRecord(
      final int kind,
      final boolean mutable,
      final Model[] srcs,
      final SlotInfo si) {
      super(kind, mutable);
      this.sources = (srcs == null) ? null : ((Model[]) srcs.clone());
      this.wrappedSlotInfo = si;
    }

    public boolean isModelDomain() {
      return sources == null;
    }

    public boolean isSrcDomain() {
      return sources != null;
    }

    /** Return a fresh copy of the array of sources. */
    public Model[] getSources() {
      return (sources == null) ? null : ((Model[]) sources.clone());
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Constructor
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * Instantiate.
   * @param partOf The Model this attribute manager is a part of.
   * @param mutex The structural lock for the model.
   */
  protected BareAttributeManager(final Model partOf, final Object mutex) {
    super(partOf, mutex);
    this.compAttrs = new HashMap<String,CompAttrRecord>();
    this.nodeAttrs = new HashMap<String,NodeAttrRecord>();
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Constructor
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin node-to-attribute methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * Get the name of the attribute identified with the given IRNode.
   * @param node The IRNode (representing an attribute) to query on.
   * @return An interned String giving the name of the attribute.
   * @exception IllegalArgumentException Thrown if the IRNode is not
   * identified with an attribute in this manager.
   */
  @Override
  public String getAttributeName(final IRNode node)
    throws UnknownAttributeException {
    String name = null;

    Iterator<String> names = compAttrs.keySet().iterator();
    while ((name == null) && names.hasNext()) {
      final String attr = names.next();
      final AttributeRecord attrRec = compAttrs.get(attr);
      if (attrRec.node.equals(node))
        name = attr;
    }
    if (name == null) {
      names = nodeAttrs.keySet().iterator();
      while ((name == null) && names.hasNext()) {
        final String attr = names.next();
        final AttributeRecord attrRec = nodeAttrs.get(attr);
        if (attrRec.node.equals(node))
          name = attr;
      }
    }
    if (name == null) {
      throw new IllegalArgumentException(
        "IRNode "
          + node
          + " does not represent an attribute in Model \""
          + partOf.getName()
          + "\".");
    }
    return name;
  }

  /**
   * Get the name-space of the attribute identified with the given IRNode.
   * @param node The IRNode (representing an attribute) to query on.
   * @return <code>true</code> iff the attribute identified with given node
   *   is a node-level attribute.
   * @exception UnknownAttributeException Thrown if the IRNode is not
   * identified with an attribute in this manager.
   */
  @Override
  public boolean isNodeAttribute(final IRNode node)
    throws UnknownAttributeException {
    boolean done = false;
    boolean isNode = false;

    Iterator<String> names = compAttrs.keySet().iterator();
    while (!done && names.hasNext()) {
      final String attr = names.next();
      final AttributeRecord attrRec = compAttrs.get(attr);
      if (attrRec.node.equals(node)) {
        isNode = false;
        done = true;
      }
    }
    if (!done) {
      names = nodeAttrs.keySet().iterator();
      while (!done && names.hasNext()) {
        final String attr = names.next();
        final AttributeRecord attrRec = nodeAttrs.get(attr);
        if (attrRec.node.equals(node)) {
          isNode = true;
          done = true;
        }
      }
    }
    if (!done) {
      throw new UnknownAttributeException(
        "IRNode "
          + node
          + " does not represent an attribute in Model \""
          + partOf.getName()
          + "\".");
    }
    return isNode;
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End node-to-attribute methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin attribute manager properties
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * Get the value of a property.  This default implementation doesn't 
   * understand any properties and always throws an exception.
   */
  @Override
  public Object getProperty(final String property) {
    throw new IllegalArgumentException(
      "Attribute managers of class "
        + this.getClass().getName()
        + " do not understand the property \""
        + property
        + "\".");
  }

  /**
   * Set the value of a property.  This default implementation doesn't 
   * understand any properties and always throws an exception.
   */
  @Override
  public void setProperty(final String property, final Object value) {
    throw new IllegalArgumentException(
      "Attribute managers of class "
        + this.getClass().getName()
        + " do not understand the property \""
        + property
        + "\".");
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End attribute manager properties
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Component-level Attribute management methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Membership in component-level attributes
  //===========================================================

  // inherit Javadoc
  @Override
  public final Iterator<String> getComponentAttributes() {
    final Set<String> copy = new HashSet<String>(compAttrs.keySet());
    return copy.iterator();
  }

  // inherit Javadoc
  @Override
  public final boolean isComponentAttribute(final String attr) {
    return compAttrs.keySet().contains(attr);
  }

  /**
   * Test if an attribute is known.  Does nothing if the attibute is not known.
   * Called exclusively for its potential exceptional side effect.
   * <p><em>The caller is expected to hold the structural lock</em>.
   *
   * @exception AttributeAlreadyExistsException Thrown if the model-level
   *            attribute is known to the attribute manager.
   */
  protected final void assertNoSuchCompAttribute(final String attr) {
    if (isComponentAttribute(attr)) {
      throw new AttributeAlreadyExistsException(attr, partOf.getName());
    }
  }

  /**
   * Test if an attribute is known.  Does nothing if the attibute is not known.
   * Called exclusively for its potential exceptional side effect.
   * <p><em>The caller is expected to hold the structural lock</em>.
   *
   * @exception AttributeAlreadyExistsException Thrown if the node-level
   *            attribute is known to the attribute manager.
   */
  protected final void assertNoSuchNodeAttribute(final String attr) {
    if (isNodeAttribute(attr)) {
      throw new AttributeAlreadyExistsException(attr, partOf.getName());
    }
  }

  //===========================================================
  //== Primary method to Add component-level attributes
  //===========================================================

  // inherit Javadoc
  @Override
  public final <T> ComponentSlot<T> addCompAttribute(
    final String attr,
    final int kind,
    final boolean isMutable,
    final ComponentSlot<T> ca,
    final AttributeChangedCallback cb) {
    final String internedAttr = attr.intern();
    ComponentSlot<T> wrapped;
    if (isMutable) {
      wrapped =
        new GuardedMutableModelAttribute<T>(structLock, ca, internedAttr, cb);
    } else {
      wrapped =
        new GuardedImmutableModelAttribute<T>(
          partOf,
          structLock,
          ca,
          internedAttr);
    }
    addCompAttributeImpl(internedAttr, kind, isMutable, wrapped);
    return wrapped;
  }

  // inherit Javadoc
  @Override
  public final void addCompAttributeImpl(
    final String attr,
    final int kind,
    final boolean isMutable,
    final ComponentSlot wrapped) {
    synchronized (structLock) {
      assertNoSuchCompAttribute(attr);
      compAttrs.put(attr, new CompAttrRecord(kind, isMutable, wrapped));
    }
  }

  //===========================================================
  //== Convienence method to Add component-level attributes
  //===========================================================

  // inherit Javadoc
  @Override
  public final <T> ComponentSlot<T> addCompAttribute(
    final String attr,
    final int kind,
    final ComponentSlot<T> ca) {
    return addCompAttribute(
      attr,
      kind,
      false,
      ca,
      AttributeChangedCallback.nullCallback);
  }

  // inherit Javadoc
  @Override
  public final <T> ComponentSlot<T> addCompAttribute(
    final String attr,
    final int kind,
    final ComponentSlot<T> ca,
    final AttributeChangedCallback cb) {
    return addCompAttribute(attr, kind, true, ca, cb);
  }

  //===========================================================
  //== Get/set info about component-level attributes
  //===========================================================

  /** 
   * Set the attribute record for a component-level attribute.
   * Caller must be synchronized on {@link #structLock}.
   */
  protected final void addCompAttrRecord(
    final String attr,
    final CompAttrRecord rec) {
    compAttrs.put(attr, rec);
  }

  /** 
   * Get the attribute record for a component-level attribute.
   * Caller must be synchronized on {@link #structLock}.
   * @return The attribute record
   * @exception UnknownAttributeException Thrown if the attribute is not found.
   */
  protected final CompAttrRecord getCompAttrRecord(final String attr) {
    final CompAttrRecord rec = compAttrs.get(attr);
    if (rec == null) {
      throw new UnknownAttributeException(attr);
    } else {
      return rec;
    }
  }

  // inherit Javadoc
  @Override
  public final int getCompAttrKind(final String attr) {
    return getCompAttrRecord(attr).kind;
  }

  // Inherit javadoc
  @Override
  public final IRNode getCompAttrNode(final String attr) {
    return getCompAttrRecord(attr).node;
  }

  // inherit Javadoc
  @Override
  public final boolean isCompAttrMutable(final String attr) {
    return getCompAttrRecord(attr).isMutable;
  }

  // inherit Javadoc
  @Override
  public final ComponentSlot getCompAttribute(final String attr) {
    return getCompAttrRecord(attr).wrapped;
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Component-level Attribute management methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin node-level Attribute management methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Membership in node-level attributes
  //===========================================================

  // inherit Javadoc
  @Override
  public final Iterator<String> getNodeAttributes() {
    final Set<String> copy = new HashSet<String>(nodeAttrs.keySet());
    return copy.iterator();
  }

  // inherit Javadoc
  @Override
  public final boolean isNodeAttribute(final String attr) {
    return nodeAttrs.keySet().contains(attr);
  }

  //===========================================================
  //== Primary method to Add node-level attributes
  //===========================================================

  // inherit Javadoc
  @Override
  public final <T> SlotInfo<T> addNodeAttribute(
    final String attr,
    final int kind,
    final boolean isMutable,
    final Model[] srcs,
    final SlotInfo<T> si,
    final AttributeChangedCallback cb) {
    final String internedAttr = attr.intern();
    SlotInfo<T> wrapped;
    if (isMutable) {
      wrapped =
        new GuardedNodeAttribute<T>(partOf, structLock, si, internedAttr, cb);
    } else {
      wrapped =
        new GuardedImmutableNodeAttribute<T>(partOf, structLock, si, internedAttr);
    }
    addNodeAttributeImpl(internedAttr, kind, isMutable, srcs, wrapped);
    return wrapped;
  }

  // inherit Javadoc
  @Override
  public final <T> SlotInfo<T> addNodeAttribute(
    final String attr,
    final int kind,
    final boolean isMutable,
    final SlotInfo<T> si,
    final AttributeChangedCallback cb) {
    return addNodeAttribute(attr, kind, isMutable, null, si, cb);
  }

  // inherit Javadoc
  @Override
  public final void addNodeAttributeImpl(
    final String attr,
    final int kind,
    final boolean isMutable,
    final Model[] srcs,
    final SlotInfo wrapped) {
    assertNoSuchNodeAttribute(attr);
    nodeAttrs.put(attr, new NodeAttrRecord(kind, isMutable, srcs, wrapped));

  }

  //===========================================================
  //== Convienence methods to Add node-level attributes
  //===========================================================

  // inherit Javadoc
  @Override
  public final <T> SlotInfo<T> addNodeAttribute(
    final String att,
    final int kind,
    final SlotInfo<T> si,
    final AttributeChangedCallback callback) {
    return addNodeAttribute(att, kind, true, si, callback);
  }

  // inherit Javadoc
  @Override
  public final <T> SlotInfo<T> addNodeAttribute(
    final String att,
    int kind,
    final SlotInfo<T> si) {
    return addNodeAttribute(
      att,
      kind,
      false,
      si,
      AttributeChangedCallback.nullCallback);
  }

  //===========================================================
  //== Get/set info about node-level attributes
  //===========================================================

  /** 
   * Set the attribute record for a node-level attribute.
   * Caller must be synchronized on {@link #structLock}.
   */
  protected final void addNodeAttrRecord(
    final String attr,
    final NodeAttrRecord rec) {
    nodeAttrs.put(attr, rec);
  }

  /**
   * Get the node-level attribute record for a give attribute.
   * @exception UnknownAttributeException Thrown if the attribute is
   * not found in the manager.
   */
  protected final NodeAttrRecord getNodeAttrRecord(final String attr) {
    final NodeAttrRecord rec = nodeAttrs.get(attr);
    if (rec == null) {
      throw new UnknownAttributeException(attr);
    } else {
      return rec;
    }
  }

  // inherit Javadoc
  @Override
  public final int getNodeAttrKind(final String attr) {
    return getNodeAttrRecord(attr).kind;
  }

  // Inherit javadoc
  @Override
  public final IRNode getNodeAttrNode(final String attr) {
    return getNodeAttrRecord(attr).node;
  }

  // inherit Javadoc
  @Override
  public final boolean isNodeAttrMutable(final String attr) {
    return getNodeAttrRecord(attr).isMutable;
  }

  // inherit Javadoc
  @Override
  public int getNodeAttrDomain(final String attr) {
    final Model[] srcs = getNodeAttrRecord(attr).sources;
    return (srcs == null) ? Model.MODEL_DOMAIN : Model.SRC_DOMAIN;
  }

  // inherit Javadoc
  @Override
  public Model[] getNodeAttrDomainSrcs(final String attr) {
    return getNodeAttrRecord(attr).getSources();
  }

  // inherit Javadoc
  @Override
  public final SlotInfo getNodeAttribute(final String attr) {
    return getNodeAttrRecord(attr).wrappedSlotInfo;
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End node-level Attribute management methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}
