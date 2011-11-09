/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/ExplicitSlotFactory.java,v 1.2 2006/03/27 21:35:50 boyland Exp $
 */
package edu.cmu.cs.fluid.ir;


/**
 * A factory that creates actual slot structures
 * @author boyland
 */
public interface ExplicitSlotFactory extends SlotFactory {
  /** Return a (possibly shared) undefined slot of the family. */
  public <T> Slot<T> undefinedSlot();
  /** Return a (possibly shared) predefined slot of the family. */
  public <T> Slot<T> predefinedSlot(T value);
}
