// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/SimpleIterator.java,v 1.9 2005/06/30 21:45:40 chance Exp $
package edu.cmu.cs.fluid.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import static edu.cmu.cs.fluid.util.IteratorUtil.noElement;

/** An Iterator class with one way to get the next element.
 */
public abstract class SimpleIterator<T> implements Iteratable<T> {
  private Object nextElement = noElement;
  public SimpleIterator() { super(); }
  /**
   * Initialize an iterator with a starting element.
   * @param initial
   */
  public SimpleIterator(T initial) { super(); nextElement = initial; }

  public boolean hasNext() {
    if (nextElement != noElement) return true;
    nextElement = computeNext();
    return nextElement != noElement;
  }

  @SuppressWarnings("unchecked")
  public T next() {
    if (nextElement != noElement) {
      try {
        return (T) nextElement;
      } finally {
        nextElement = noElement;
      }
    } else {
      Object next = computeNext();
      if (next == noElement) throwException();
      return (T) next;
    }
  }

  /** Return the next element or return noElement */
  protected abstract Object computeNext();

  protected void throwException() {
    throw new NoSuchElementException("end of Iterator");
  }
  
  public final Iterator<T> iterator() {
    return this;
  }
}
