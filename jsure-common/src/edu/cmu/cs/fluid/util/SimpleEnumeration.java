/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/SimpleEnumeration.java,v 1.7 2005/05/25 18:03:35 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/** An enumeration class with one way to get the next element.
 */
@Deprecated
@SuppressWarnings("all")
public abstract class SimpleEnumeration implements Enumeration {
  public SimpleEnumeration() { super(); }
  protected final Object noElement = this; // used to indicate no element
  protected Object nextElement = noElement;

  public final boolean hasMoreElements() {
    if (nextElement != noElement) return true;
    nextElement = computeNextElement();
    return nextElement != noElement;
  }

  public Object nextElement() {
    if (nextElement != noElement) {
      try {
	return nextElement;
      } finally {
	nextElement = noElement;
      }
    } else {
      Object element = computeNextElement();
      if (element == noElement) throwException();
      return element;
    }
  }

  /** Return the next element or return noElement */
  protected abstract Object computeNextElement();

  protected void throwException() {
    throw new NoSuchElementException("end of enumeration");
  }
}
