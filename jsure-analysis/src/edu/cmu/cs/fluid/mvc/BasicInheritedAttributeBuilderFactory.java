/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/BasicInheritedAttributeBuilderFactory.java,v 1.8 2006/03/30 19:47:20 chance Exp $ */
package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.mvc.attributes.*;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * Factory returning an attribute builder for inheriting attributes in a
 * pass-through manner, supporting the inheritance modes:
 * {@link AttributeInheritanceManager#MUTABLE_LOCAL},
 * {@link AttributeInheritanceManager#MUTABLE_SOURCE}, and
 * {@link AttributeInheritanceManager#IMMUTABLE}.  
 *
 * <p><em>Caveat</em>: Immutable attributes do not prevent the structure of the
 * attribute value from being changed, for example if the attribute is
 * sequence-values, the sequence may still be mutable.  This seems wrong.
 */
public final class BasicInheritedAttributeBuilderFactory
  implements BareAttributeInheritanceManager.InheritedAttributeBuilderFactory {
  /**
   * Prototype reference to the factory.
   */
  public static final BareAttributeInheritanceManager
    .InheritedAttributeBuilderFactory prototype =
    new BasicInheritedAttributeBuilderFactory();

  /**
   * Singleton reference to the attribute builder returned by the factory.
   */
  private static final BasicInheritedAttributeBuilder singletonBuilder =
    new BasicInheritedAttributeBuilder();

  /**
   * Private constructor to enforce the singleton pattern.  Use the
   * reference in {@link #prototype} instead.
   */
  private BasicInheritedAttributeBuilderFactory() {
  }

  // inherit javadoc.
  @Override
  public BareAttributeInheritanceManager.InheritedAttributeBuilder create() {
    return singletonBuilder;
  }
}

/**
 * Attribute builder for inheriting attributes in a pass-through manner,
 * supporting the inheritance modes:
 * {@link AttributeInheritanceManager#MUTABLE_LOCAL},
 * {@link AttributeInheritanceManager#MUTABLE_SOURCE}, and
 * {@link AttributeInheritanceManager#IMMUTABLE}.  
 *
 * <p><em>Caveat</em>: Immutable attributes do not prevent the structure of the
 * attribute value from being changed, for example if the attribute is
 * sequence-values, the sequence may still be mutable.  This seems wrong.
 */
final class BasicInheritedAttributeBuilder
  implements BareAttributeInheritanceManager.InheritedAttributeBuilder {
  /**
   * Generates an attribute wrapped by one of 
   * {@link MutableLocalInheritedModelAttribute},
   * {@link GuardedMutableModelAttribute}, or
   * {@link GuardedImmutableModelAttribute}.
   */
  @Override
  public <T> ComponentSlot<T> buildCompAttribute(
    final Model partOf,
    final Object mutex,
    final String attr,
    final Object mode,
    final ComponentSlot<T> ca,
    final AttributeChangedCallback cb) {
    ComponentSlot<T> wrapped = null;
    if (mode == AttributeInheritanceManager.MUTABLE_LOCAL) {
      wrapped =
        new MutableLocalInheritedModelAttribute<T>(
          mutex,
          attr,
          ca,
          SimpleComponentSlotFactory.simplePrototype,
          cb);
    } else if (mode == AttributeInheritanceManager.MUTABLE_SOURCE) {
      wrapped = new GuardedMutableModelAttribute<T>(mutex, ca, attr, cb);
    } else if (mode == AttributeInheritanceManager.IMMUTABLE) {
      wrapped = new GuardedImmutableModelAttribute<T>(partOf, mutex, ca, attr);
    } else {
      throw new IllegalArgumentException("Unknown inheritance mode: " + mode);
    }
    return wrapped;
  }

  /**
   * Generates an attribute wrapped by one of 
   * {@link MutableLocalInheritedNodeAttribute},
   * {@link GuardedNodeAttribute}, or
   * {@link GuardedImmutableNodeAttribute}.
   */
  @Override
  public <T> SlotInfo<T> buildNodeAttribute(
    final Model partOf,
    final Object mutex,
    final String attr,
    final Object mode,
    final SlotInfo<T> si,
    final AttributeChangedCallback cb) {
    SlotInfo<T> wrapped = null;
    if (mode == AttributeInheritanceManager.MUTABLE_LOCAL) {
      wrapped =
        new MutableLocalInheritedNodeAttribute<T>(
          partOf,
          mutex,
          attr,
          si,
          SimpleSlotFactory.prototype,
          cb);
    } else if (mode == AttributeInheritanceManager.MUTABLE_SOURCE) {
      wrapped = new GuardedNodeAttribute<T>(partOf, mutex, si, attr, cb);
    } else if (mode == AttributeInheritanceManager.IMMUTABLE) {
      wrapped = new GuardedImmutableNodeAttribute<T>(partOf, mutex, si, attr);
    } else {
      throw new IllegalArgumentException("Unknown inheritance mode: " + mode);
    }
    return wrapped;
  }

  /**
   * Considers the modes {@link AttributeInheritanceManager#MUTABLE_LOCAL} and
   * {@link AttributeInheritanceManager#MUTABLE_SOURCE}
   * to be mutable.
   */
  @Override
  public boolean isModeMutable(final Object mode) {
    return (mode == AttributeInheritanceManager.MUTABLE_LOCAL)
      || (mode == AttributeInheritanceManager.MUTABLE_SOURCE);
  }
}
