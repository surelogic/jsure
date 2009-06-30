/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/ConstantSlot.java,v 1.5 2006/03/27 21:35:50 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

/** A slot that cannot change value.  May be shared or may not be.
 * @typeparam Value
 */
public class ConstantSlot<T> extends PredefinedSlot<T> implements Slot<T> {
  public ConstantSlot(T initial) {
    super(initial);
  }

  public Slot<T> setValue(T newValue) throws SlotImmutableException {
    throw new SlotImmutableException("constant slots are immutable");
  }
}