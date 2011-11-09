/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/UndefinedSimpleSlot.java,v 1.5 2006/03/27 21:35:50 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

/** A class that holds the place for a value,
 * but throws an exception if not set before used.
 * When set, it turns into a SimpleSlot
 * @see SimpleSlot
 */
public class UndefinedSimpleSlot<T> extends UndefinedSlot<T> {
  public static UndefinedSimpleSlot prototype = new UndefinedSimpleSlot();
  public Slot<T> setValue(T newValue) {
    return new SimpleSlot<T>(newValue);
  }
}