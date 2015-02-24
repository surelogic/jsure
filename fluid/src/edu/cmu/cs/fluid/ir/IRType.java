/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRType.java,v 1.9 2006/03/27 21:35:50 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.util.Comparator;

import com.surelogic.ThreadSafe;

/** The interface for descriptors of the types of (storable)
 * slots.
 * @see SlotInfo
 */
@ThreadSafe
public interface IRType<T> {
  boolean isValid(Object value);
  
  /**
   * Get the Comparator for elements of this type (if one exists). 
   * Reminder: Comparators are for <em>total</em> orders only.
   * @return The comparator for the type, if the type can be ordered;
   *         <code>null</code> if the type has no comparator.
   */
  Comparator<T> getComparator();

  /** Write a value out. */
  void writeValue(T value, IROutput out) throws IOException;
  
  /** Read a value in. */
  T readValue(IRInput in) throws IOException;

  /** Write the type out (starting with a registered byte). */
  void writeType(IROutput out) throws IOException;

  /** Read a type in continuing after the registered byte. */
  IRType<T> readType(IRInput in) throws IOException;

  /** make value (of some IRType) given in string */
  public T fromString(String s);

  /** covert value o (of some IRType) to String */
  public String toString(T o);
}
