/*
 * SimpleComponentSlotFactory.java
 *
 * Created on December 11, 2001, 9:31 AM
 */

package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.ir.ConstantExplicitSlotFactory;
import edu.cmu.cs.fluid.ir.IRType;
import edu.cmu.cs.fluid.ir.SimpleExplicitSlotFactory;
import edu.cmu.cs.fluid.ir.ExplicitSlotFactory;

/**
 * Creates ComponentSlotFactories that return ComponentSlots implemented
 * by {@link SimpleComponentSlot}.
 *
 * @author Aaron Greenhouse
 */
public class SimpleComponentSlotFactory implements ComponentSlot.Factory {
  /**
   * Prototype factory that always creates ComponentSlots using a
   * {@link edu.cmu.cs.fluid.ir.SimpleSlotFactory}.
   */
  public static final ComponentSlot.Factory simplePrototype =
    new SimpleComponentSlotFactory(SimpleExplicitSlotFactory.prototype);

  /**
   * Prototype factory that always creates ComponentSlots using a
   * {@link edu.cmu.cs.fluid.ir.ConstantSlotFactory}.
   */
  public static final ComponentSlot.Factory constantPrototype =
    new SimpleComponentSlotFactory(ConstantExplicitSlotFactory.prototype);

  /**
   * Prototype factory that always creates ComponentSlots using a
   * {@link edu.cmu.cs.fluid.version.VersionedSlotFactory}.
   */
  public static final ComponentSlot.Factory versionedPrototype =
    new SimpleComponentSlotFactory(
      edu.cmu.cs.fluid.version.VersionedSlotFactory.prototype);

  /** The SlotFactory used to create the underlying storage. */
  private final ExplicitSlotFactory slotFactory;

  /**
   * Creates new SimpleComponentSlotFactory that creates ComponentSlots
   * based on the provided SlotFactory.
   * @param sf The SlotFactory to use to create the underlying storage.
   */
  public SimpleComponentSlotFactory(final ExplicitSlotFactory sf) {
    slotFactory = sf;
  }

  @Override
  public <T> ComponentSlot<T> undefinedSlot(final IRType<T> type) {
    return new SimpleComponentSlot<T>(type, slotFactory);
  }

  @Override
  public <T> ComponentSlot<T> predefinedSlot(final IRType<T> type, final T value) {
    if (!type.isValid(value)) {
      throw new IllegalArgumentException("Initial value is not of appropriate IRType.");
    }
    return new SimpleComponentSlot<T>(type, slotFactory, value);
  }
}
