/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/UndefinedSlot.java,v 1.7 2006/03/27 21:35:50 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

/** A class that holds the place for a value,
 * but throws an exception if not replaced before used.
 */
public abstract class UndefinedSlot<T> extends AbstractSlot<T> {
  @Override
  public T getValue() {
    throw new SlotUndefinedException();
  }
  @Override
  public boolean isValid() {
    return false;
  }
}