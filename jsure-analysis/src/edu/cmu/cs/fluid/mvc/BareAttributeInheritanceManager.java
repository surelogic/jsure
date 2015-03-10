/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/BareAttributeInheritanceManager.java,v 1.7 2006/03/29 18:30:56 chance Exp $ */
package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * @author Aaron Greenhouse
 */
@SuppressWarnings("unchecked")
public class BareAttributeInheritanceManager
  extends ModelPart
  implements AttributeInheritanceManager {
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Fields 
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * The attribute manager of the model whose attribute inheritnace is
   * being managed by this instance.
   */
  private final AttributeManager attrManager;

  /**
   * Reference to strategy used to construct inherited attributes.
   */
  private final InheritedAttributeBuilder inheritedAttrBuilder;

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
  //== Interface for Inherited attribute builder
  //===========================================================

  /**
   * Interface for strategies that build wrappers and records for
   * inherited attributes.
   */
  public static interface InheritedAttributeBuilder {
    /**
     * Create a wrapped model-level attribute that accomplishes the
     * desired inheritance effects based on the given inheritance mode.
     * @param partOf The model that inherited attribute is to be added to.
     * @param mutex The lock protecting the model that attribute is to be added to.
     * @param attr The name the inherited attribute should have.
     * @param mode The inheritance mode to use.
     * @param ca The attribute storage to wrap.
     * @param cb The attribute changed callback to use for local modifications
     *           to the inherited attribute.
     * @return The wrapped attribute, or <code>null</code> if the attribute
     * should not be inherited.
     */
    public <T> ComponentSlot<T> buildCompAttribute(
      Model partOf,
      Object mutex,
      String attr,
      Object mode,
      ComponentSlot<T> ca,
      AttributeChangedCallback cb);

    /**
     * Create a wrapped node-level attribute that accomplished the desired
     * inheritance effects based on the given inheritance mode.
     * @param partOf The model that inherited attribute is to be added to.
     * @param mutex The lock protecting the model that attribute is to be added to.
     * @param attr The name the inherited attribute should have.
     * @param mode The inheritance mode to use.
     * @param si The attribute storage to wrap.
     * @param cb The attribute changed callback to use for local modifications
     *           to the inherited attribute.
     * @return The wrapped attribute, or <code>null</code> if the attribute
     * should not be inherited.
     */
    public <T> SlotInfo<T> buildNodeAttribute(
      Model partOf,
      Object mutex,
      String attr,
      Object mode,
      SlotInfo<T> si,
      AttributeChangedCallback cb);

    /**
     * Query whether an inheritance mode should be treated as allowing
     * the inherited attribute to be locally mutable by clients of the model.
     */
    public boolean isModeMutable(Object mode);
  }

  /**
   * Interface for inherted attribute builder factories.
   */
  public static interface InheritedAttributeBuilderFactory {
    /** 
     * Return a reference to a suitable attribute builder.
     */
    public InheritedAttributeBuilder create();
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Constructor
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * Instantiate.  For subclasses.  Model implementors should use 
   * an appropriate {@link edu.cmu.cs.fluid.mvc.AttributeInheritanceManager.Factory}.
   * @param partOf The Model this attribute manager is a part of.
   * @param mutex The structural lock for the model.
   * @param attrManager The attribute manager of the model.
   * @param builderFactory Factory returning the strategy to use
   *        for wrapping inherited attributes.
   */
  public BareAttributeInheritanceManager(
    final Model partOf,
    final Object mutex,
    final AttributeManager attrManager,
    final InheritedAttributeBuilderFactory builderFactory) {
    super(partOf, mutex);
    this.attrManager = attrManager;
    this.inheritedAttrBuilder = builderFactory.create();
  }

  // inherit javadoc
  @Override
  public final boolean inheritCompAttribute(
    final Model srcModel,
    final String srcAttr,
    final String attr,
    final Object mode,
    final int kind,
    final AttributeChangedCallback cb) {
    final ComponentSlot wrapped =
      inheritedAttrBuilder.buildCompAttribute(
        partOf,
        structLock,
        attr,
        mode,
        srcModel.getCompAttribute(srcAttr),
        cb);
    if (wrapped != null) {
      attrManager.addCompAttributeImpl(
        attr,
        kind,
        inheritedAttrBuilder.isModeMutable(mode),
        wrapped);
      return true;
    } else {
      return false;
    }
  }

  // inherit javadoc
  @Override
  public final boolean inheritNodeAttribute(
    final Model srcModel,
    final String srcAttr,
    final String attr,
    final Object mode,
    final int kind,
    final AttributeChangedCallback cb) {
    final SlotInfo wrapped =
      inheritedAttrBuilder.buildNodeAttribute(
        partOf,
        structLock,
        attr,
        mode,
        srcModel.getNodeAttribute(srcAttr),
        cb);
    if (wrapped != null) {
      attrManager.addNodeAttributeImpl(
        attr,
        kind,
        inheritedAttrBuilder.isModeMutable(mode),
        null,
        wrapped);
      return true;
    } else {
      return false;
    }
  }

  // inherit javadoc
  @Override
  public final void inheritAttributesFromModel(
    final Model srcModel,
    final AttributeInheritancePolicy policy,
    final AttributeChangedCallback cb) {
    // should sync to get a stable view of the attributes of the src model
    final AttributeInheritancePolicy.HowToInherit[] compAttrs =
      policy.compAttrsToInherit(srcModel);
    final AttributeInheritancePolicy.HowToInherit[] nodeAttrs =
      policy.nodeAttrsToInherit(srcModel);

    synchronized (structLock) {
      for (int i = 0; i < compAttrs.length; i++) {
        inheritCompAttribute(
          srcModel,
          compAttrs[i].attr,
          compAttrs[i].inheritAs,
          compAttrs[i].mode,
          compAttrs[i].kind,
          cb);
      }
      for (int i = 0; i < nodeAttrs.length; i++) {
        inheritNodeAttribute(
          srcModel,
          nodeAttrs[i].attr,
          nodeAttrs[i].inheritAs,
          nodeAttrs[i].mode,
          nodeAttrs[i].kind,
          cb);
      }
    }
  }

  /**
   * Get the value of a property.  This implemenation simply delegates to the
   * associated AttributeManager.
   * @param property The name of the property as in interned String
   *                whose value is to be returned.
   * @return The value of the property.
   * @exception IllegalArgumentException Thrown if the property is not
   *           understood.  This seems like a better idea than returning null
   *           because properties are intended to deal with special cases
   *           of attribute manager implementations, and therefore trying to
   *           get the wrong property would indicate an error in the
   *           implementation of the Model that is using the manager.
   */
  @Override
  public Object getProperty(final String property) {
    return attrManager.getProperty(property);
  }

  /**
   * Set the value of a property.  This implemenation simply delegates to the
   * associated AttributeManager.
   * @param property The name of the property as in interned String
   *                whose value is to be set.
   * @param value The new value.
   * @exception IllegalArgumentException Thrown if the property is not
   *           understood.  This seems like a better idea than silently
   *           failing because properties are intended to deal with special
   *           cases of attribute manager implementations, and therefore
   *           trying to set the wrong property would indicate an error in the
   *           implementation of the Model that is using the manager.
   *
   *           <p>May also be thrown if the value is unacceptable.
   */
  @Override
  public void setProperty(final String property, final Object value) {
    attrManager.setProperty(property, value);
  }
}
