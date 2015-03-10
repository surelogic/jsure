/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/PredefinedSlot.java,v 1.10 2006/03/27 21:35:50 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.PrintStream;

/** A class that holds a default value until it is set.
 * @see UndefinedSlot
 * @typeparam Value
 */
public abstract class PredefinedSlot<T> extends AbstractSlot<T> {
  /** Initial value of the slot.
   * @type Value
   */
  protected T startValue;

  /** Create a new predefined slot.
   * @param value initial value
   * <dl purpose=fluid>
   *   <dt>type<dd> Value
   * </dl>
   */
  public PredefinedSlot(T value) {
    startValue = value;
  }

  /** Get the value of a predefined slot.
   * @return initial value
   * <dl purpose=fluid>
   *   <dt>type<dd> Value
   * </dl>
   */
  public T getValue() {
    return startValue;
  }

  public boolean isValid() {
    return true;
  }

  @Override
  public void describe(PrintStream out) {
    super.describe(out);
    out.println("   value = " + startValue);
  }
}
