/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SimpleSlot.java,v 1.11 2007/07/05 18:15:15 aarong Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.io.PrintStream;

/** A simple container: the value simply is stored as a field.
 * No versioning.
 * @typeparam Value
 */
public class SimpleSlot<T> extends AbstractSlot<T> {
  /** The value that is fetched and modified.
   * @type Value
   */
  private T value;
  private boolean dirty;

  public SimpleSlot(T initial) {
    value = initial;
    dirty = true; /// unlike predefined slot
  }

  public T getValue() {
    return value;
  }

  public Slot<T> setValue(T newValue) {
    value = newValue;
    dirty = true;
    return this;
  }

  public boolean isValid() {
    return true;
  }

  @Override
  public boolean isChanged() {
    return dirty;
  }

  @Override
  public void writeValue(IRType<T> ty, IROutput out) 
     throws IOException
  {
    super.writeValue(ty,out);
    dirty = false;
  }

  @Override
  public Slot<T> readValue(IRType<T> ty, IRInput in) 
     throws IOException
  {
    value = ty.readValue(in);
    dirty = false;
    return this;
  }

  @Override
  public void describe(PrintStream out) {
    super.describe(out);
    out.println("    value = " + value);
  }
}
