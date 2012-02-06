/*$Header: /cvs/fluid/fluid/src/com/surelogic/util/NullCollection.java,v 1.1 2008/01/18 23:10:45 aarong Exp $*/
package com.surelogic.util;

import java.util.Collection;
import java.util.Iterator;

import edu.cmu.cs.fluid.util.ArrayUtil;
import edu.cmu.cs.fluid.util.EmptyIterator;

/**
 * A {@link java.util.Collection} implementation that is always empty, and ignores any
 * elements that are added to it.  The primary purpose of this class is to pass 
 * an instance of it to methods that populate collections as a side-effect in the case
 * when the caller doesn't care about those particular results.
 */
public class NullCollection<E> implements Collection<E> {
  @SuppressWarnings("rawtypes")
  private static final NullCollection prototype = new NullCollection();
  
  
  
  // Don't complain about unchecked type paramteer and the supposedly useless cast
  @SuppressWarnings({ "cast", "unchecked" })
  public static <T> NullCollection<T> prototype() {
    return (NullCollection<T>) prototype;
  }
  
  
  
  // Enforce singleton.  Package visible so the class can be extended.
  NullCollection() {
    // Nothing to do
  }
  
  public final boolean add(final E o) {
    return false;
  }

  public final boolean addAll(final Collection<? extends E> c) {
    return false;
  }

  public final void clear() {
    // Nothing to do
  }

  public final boolean contains(final Object o) {
    return false;
  }

  public final boolean containsAll(final Collection<?> c) {
    return false;
  }

  public final boolean isEmpty() {
    return true;
  }

  public final Iterator<E> iterator() {
    return new EmptyIterator<E>();
  }

  public final boolean remove(final Object o) {
    return false;
  }

  public final boolean removeAll(final Collection<?> c) {
    return false;
  }

  public final boolean retainAll(final Collection<?> c) {
    return false;
  }

  public final int size() {
    return 0;
  }

  public final Object[] toArray() {
    return ArrayUtil.empty;
  }

  public final <T> T[] toArray(final T[] a) {
    if (a.length > 0) a[0] = null;
    return a;
  }

  @Override
  public final String toString() {
    return "[]";
  }
}
