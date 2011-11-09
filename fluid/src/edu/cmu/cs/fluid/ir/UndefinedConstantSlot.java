/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/UndefinedConstantSlot.java,v 1.5 2006/03/27 21:35:50 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

/** A class that holds the place for a constant value,
 * but throws an exception if not set before used.
 * When set, it turns into a ConstantSlot.
 * @see ConstantSlot
 */
public class UndefinedConstantSlot<T> extends UndefinedSlot<T> {
  public static UndefinedConstantSlot prototype = new UndefinedConstantSlot();
  public Slot<T> setValue(T newValue) {
    return new ConstantSlot<T>(newValue);
  }
}