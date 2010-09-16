/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/SingletonSet.java,v 1.4 2007/07/05 18:15:13 aarong Exp $
 */
package edu.cmu.cs.fluid.util;

import java.util.*;

/**
 * An immutable set that, when grown, returns a mutable class.
 * @author boyland
 */
public class SingletonSet<T> extends AbstractSet<T> implements PossiblyImmutableSet<T> {

  private final T element;
  
  /** Create a singleton set with one (non-null) element.
   * @param e element to initialize
   */
  public SingletonSet(T e) {
    super();
    if (e == null) throw new NullPointerException("SingletonSet requires non-null element");
    element = e;
  }

  /* (non-Javadoc)
   * @see java.util.Collection#size()
   */
  @Override
  public int size() {
    return 1;
  }

  /* (non-Javadoc)
   * @see java.util.Collection#iterator()
   */
  @Override
  public Iterator<T> iterator() {
    return new SingletonIterator<T>(element);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object o) {
    if (o instanceof Set) {
      Set s = (Set) o;
      if (s.size() == 1 && s.contains(element)) {
        return true;
      }
    }
    return false;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return element.hashCode();
  }
  
  /* (non-Javadoc)
   * @see java.util.Collection#contains(java.lang.Object)
   */
  @Override
  public boolean contains(Object o) {
    return element.equals(o);
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.util.PossiblyImmutableSet#addCopy(java.lang.Object)
   */
  public PossiblyImmutableSet<T> addCopy(T e) {
    if (element.equals(e)) return this;
    return new MutableSet<T>(this).addCopy(e);
  }

}
