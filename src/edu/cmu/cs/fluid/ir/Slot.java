/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/Slot.java,v 1.10 2006/03/27 21:35:50 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.io.PrintStream;

/** An indirect reference to another value, an <em>explicit</em> slot.
 * @see UndefinedSlot
 * @see PredefinedSlot
 * @typeparam Value
 */

public interface Slot<T> {
  /** Get the value.
   * @return the value represented indirectly.
   *  <dl purpose=fluid>
   *    <dt>type<dd> Value
   *  </dl>
   * @precondition isValid()
   * @exception SlotUndefinedException
   *  If the slot is not initialized with a value.
   */
  public T getValue() throws SlotUndefinedException;

  /** Set the value stored here.
   * The result of this method replaces this object.
   * @param newValue the value to store.
   *  <dl purpose=fluid>
   *    <dt>type<dd> Value
   *  </dl>
   * @return slot to replace this one.
   * @exception SlotImmutableException
   *  If the slot is not mutable.
   */
  public Slot<T> setValue(T newValue) throws SlotImmutableException;

  /** Return true if there is a valid value */
  public boolean isValid();

  /** Has this slot changed since its "previous value" ("initial value") ? */
  public boolean isChanged();

  /** Write the slot's value to the output stream. */
  public void writeValue(IRType<T> ty, IROutput out) throws IOException;

  /** Read a new slot's value from the input stream and return a new slot. */
  public Slot<T> readValue(IRType<T> ty, IRInput in) throws IOException;

  /** Describe information about the slot for debugging **/
  public void describe(PrintStream out);
}
