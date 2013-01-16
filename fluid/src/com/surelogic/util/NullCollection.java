/*$Header: /cvs/fluid/fluid/src/com/surelogic/util/NullCollection.java,v 1.1 2008/01/18 23:10:45 aarong Exp $*/
package com.surelogic.util;

import java.util.Collection;
import java.util.Iterator;

import com.surelogic.common.SLUtility;

import edu.cmu.cs.fluid.util.EmptyIterator;
import com.surelogic.Starts;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;
import com.surelogic.Borrowed;

/**
 * A {@link java.util.Collection} implementation that is always empty, and ignores any
 * elements that are added to it.  The primary purpose of this class is to pass 
 * an instance of it to methods that populate collections as a side-effect in the case
 * when the caller doesn't care about those particular results.
 */
public class NullCollection<E> implements Collection<E> {
  @SuppressWarnings("rawtypes")
  private static final NullCollection prototype = new NullCollection();
  
  
  
  // Don't complain about unchecked type parameter and the supposedly useless cast
  @SuppressWarnings({ "cast", "unchecked" })
  public static <T> NullCollection<T> prototype() {
    return (NullCollection<T>) prototype;
  }
  
  
  
  // Enforce singleton.  Package visible so the class can be extended.
  NullCollection() {
    // Nothing to do
  }
  
  @Override
  public final boolean add(final E o) {
    return false;
  }

  @Override
  public final boolean addAll(final Collection<? extends E> c) {
    return false;
  }

  @Override
  @Borrowed("this")
  @RegionEffects("writes Instance")
  @Starts("nothing")
  public final void clear() {
    // Nothing to do
  }

  @Override
  @Borrowed("this")
  @RegionEffects("reads o:Instance, Instance")
  @Starts("nothing")
  public final boolean contains(@Borrowed final Object o) {
    return false;
  }

  @Override
  @Borrowed("this")
  @RegionEffects("reads c:Instance, Instance")
  @Starts("nothing")
  public final boolean containsAll(@Borrowed final Collection<?> c) {
    return false;
  }

  @Override
  @Borrowed("this")
  @RegionEffects("reads Instance")
  @Starts("nothing")
  public final boolean isEmpty() {
    return true;
  }

  @Override
  @Borrowed("this")
  @RegionEffects("writes Instance")
  @Unique("return")
  @Starts("nothing")
  public final Iterator<E> iterator() {
    return new EmptyIterator<E>();
  }

  @Override
  @Borrowed("this")
  @RegionEffects("reads o:Instance; writes Instance")
  @Starts("nothing")
  public final boolean remove(@Borrowed final Object o) {
    return false;
  }

  @Override
  @Borrowed("this")
  @RegionEffects("reads c:Instance; writes Instance")
  @Starts("nothing")
  public final boolean removeAll(@Borrowed final Collection<?> c) {
    return false;
  }

  @Override
  @Borrowed("this")
  @RegionEffects("reads c:Instance; writes Instance")
  @Starts("nothing")
  public final boolean retainAll(@Borrowed final Collection<?> c) {
    return false;
  }

  @Override
  @Borrowed("this")
  @RegionEffects("reads Instance")
  @Starts("nothing")
  public final int size() {
    return 0;
  }

  @Override
  @Borrowed("this")
  @RegionEffects("reads Instance")
  @Unique("return")
  @Starts("nothing")
  public final Object[] toArray() {
    return SLUtility.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public final <T> T[] toArray(final T[] a) {
    if (a.length > 0) a[0] = null;
    return a;
  }

  @Override
  public final String toString() {
    return "[]";
  }
}
