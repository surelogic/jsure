/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/PredefinedSimpleSlot.java,v 1.4 2006/03/27 21:35:50 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

/** A shared container.  When set a copy is introduced.
 * No versioning.
 * @typeparam Value
 * @see SimpleSlot
 * @see UndefinedSimpleSlot
 */
public class PredefinedSimpleSlot<T> extends PredefinedSlot<T> {
  public PredefinedSimpleSlot(T initial) {
    super(initial);
  }
  public Slot<T> setValue(T newValue) {
    return new SimpleSlot<T>(newValue);
  }
}
