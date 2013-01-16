/*$Header: /cvs/fluid/fluid/src/com/surelogic/util/NullSet.java,v 1.1 2008/01/18 23:10:45 aarong Exp $*/
package com.surelogic.util;

import java.util.Set;
import com.surelogic.Starts;
import com.surelogic.RegionEffects;
import com.surelogic.Borrowed;

/**
 * A {@link java.util.Set} implementation that is always empty, and ignores any
 * elements that are added to it.  The primary purpose of this class is to pass 
 * an instance of it to methods that populate lists as a side-effect in the case
 * when the caller doesn't care about those particular results.
 * TODO Fill in purpose.
 * @author aarong
 */
public final class NullSet<E> extends NullCollection<E> implements Set<E> {
  @SuppressWarnings("rawtypes")
  private static final NullSet prototype = new NullSet();
  
  @SuppressWarnings({ "cast", "unchecked" })
  public static <T> NullSet<T> prototype() {
    return (NullSet<T>) prototype;
  }
  
  private NullSet() {
    // Nothing to do
  }
  
  @SuppressWarnings("rawtypes")
  @Borrowed("this")
  @RegionEffects("reads o:Instance, Instance")
  @Starts("nothing")
  @Override
  public boolean equals(@Borrowed final Object o) {
    return (o == this) || ((o instanceof Set) && (((Set) o).size() == 0));
  }

  @Borrowed("this")
  @RegionEffects("reads Instance")
  @Starts("nothing")
  @Override
  public final int hashCode() {
    return 0;
  }
}
