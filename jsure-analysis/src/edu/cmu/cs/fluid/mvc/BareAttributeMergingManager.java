/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/BareAttributeMergingManager.java,v 1.7 2006/03/30 19:47:20 chance Exp $ */

package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * Basic implementation of {@link AttributeMergingManager};
 * Implements the general framework for inheriting an attribute based on the
 * policy, leaving abstract the mechanism by which the inherited attribute is
 * wrapped.
 *
 * <p>Say more here...
 *
 * @author Aaron Greenhouse
 */
public class BareAttributeMergingManager
  extends ModelPart
  implements AttributeMergingManager {
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Fields 
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * The attribute manager of the model whose attribute merging is
   * being managed by this instance.
   */
  private final AttributeManager attrManager;

  /**
   * Reference to strategy used to construct inherited attributes.
   */
  private final MergedAttributeBuilder mergedAttrBuilder;

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
   * merge-inherited attributes.  Implementations need not support
   * arbitrary numbers of source models; that is, it is acceptable for
   * an implementation to only support a fixed-number of source models.
   */
  public static interface MergedAttributeBuilder {
    /**
     * Generate a merged component-level attribute.
     * @exception IllegalArgumentException Thrown if the inheritance mode
     * is not applicable or if the attribute is not present in all the 
     * given models.
     */
    public <T> ComponentSlot<T> buildCompAttribute(
      Model partOf,
      Object mutex,
      String attr,
      Object mode,
      ComponentSlot<T>[] attrs,
      AttributeChangedCallback cb);

    /**
     * Generate a merged node-level attribute.
     * @exception IllegalArgumentException Thrown if the inheritance mode
     * is not applicable.
     */
    public <T> SlotInfo<T> buildNodeAttribute(
      Model partOf,
      Object mutex,
      String attr,
      Object mode,
      SlotInfo<T>[] attrs,
      AttributeChangedCallback cb);

    /**
     * Query if a given mode is considered to be mutable.
     */
    public boolean isModeMutable(Object mode);
  }

  /**
   * Interface for merged attribute builder factories.
   */
  public static interface MergedAttributeBuilderFactory {
    /** 
     * Return a reference to a suitable attribute builder.
     */
    public MergedAttributeBuilder create();
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Inner classes
  //-----------------------------------------------------------
  //-----------------------------------------------------------

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
  public BareAttributeMergingManager(
    final Model partOf,
    final Object mutex,
    final AttributeManager attrManager,
    final MergedAttributeBuilderFactory builderFactory) {
    super(partOf, mutex);
    this.attrManager = attrManager;
    this.mergedAttrBuilder = builderFactory.create();
  }

  // inherit javadoc
  @Override
  public final boolean mergeCompAttributes(
    final Model[] srcModels,
    final String[] srcAttrs,
    final String attr,
    final Object mode,
    final int kind,
    final AttributeChangedCallback cb) {
    if (srcModels.length != srcAttrs.length) {
      throw new IllegalArgumentException(
        "Length of srcModels is "
          + srcModels.length
          + " and does not equal the length of srcAttrs ("
          + srcAttrs.length
          + ").");
    }

    final ComponentSlot[] slots = new ComponentSlot[srcModels.length];
    for (int i = 0; i < slots.length; i++) {
      slots[i] = srcModels[i].getCompAttribute(srcAttrs[i]);
    }

    final ComponentSlot wrapped =
      mergedAttrBuilder.buildCompAttribute(
        partOf,
        structLock,
        attr,
        mode,
        slots,
        cb);
    if (wrapped != null) {
      attrManager.addCompAttributeImpl(
        attr,
        kind,
        mergedAttrBuilder.isModeMutable(mode),
        wrapped);
      return true;
    } else {
      return false;
    }
  }

  // inherit javadoc
  @Override
  public final boolean mergeNodeAttributes(
    final Model[] srcModels,
    final String[] srcAttrs,
    final String attr,
    final Object mode,
    final int kind,
    final AttributeChangedCallback cb) {
    if (srcModels.length != srcAttrs.length) {
      throw new IllegalArgumentException(
        "Length of srcModels is "
          + srcModels.length
          + " and does not equal the length of srcAttrs ("
          + srcAttrs.length
          + ").");
    }

    final SlotInfo[] infos = new SlotInfo[srcModels.length];
    for (int i = 0; i < infos.length; i++) {
      infos[i] = srcModels[i].getNodeAttribute(srcAttrs[i]);
    }

    final SlotInfo wrapped =
      mergedAttrBuilder.buildNodeAttribute(
        partOf,
        structLock,
        attr,
        mode,
        infos,
        cb);
    if (wrapped != null) {
      attrManager.addNodeAttributeImpl(
        attr,
        kind,
        mergedAttrBuilder.isModeMutable(mode),
        null,
        wrapped);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Object getProperty(final String property) {
    return attrManager.getProperty(property);
  }

  @Override
  public void setProperty(final String property, final Object value) {
    attrManager.setProperty(property, value);
  }
}
