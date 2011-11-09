/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/CachedSet.java,v 1.8 2007/10/10 02:09:12 boyland Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** A simple implementation of AbstractCachedSet with a static hashtable.
 */
@SuppressWarnings("unchecked")
public final class CachedSet<T> extends AbstractCachedSet<T> {
  private final int generation = current_generation;
  private static int current_generation = 0;

  protected CachedSet(T[] elements, boolean inverse) {
    super(elements,inverse);
  }

  @Override
  public boolean equals(AbstractCachedSet other) {
    /* The original definition (in AbstractCachedSet) doesn't
     * work because it assumes that the table storing an instance
     * can be reached from the instance.  Instead we must
     * use generations.
     */
    if (other instanceof CachedSet &&
	((CachedSet)other).generation == generation)
      return other == this;
    else // we need super^2 to implement without the new static method:
      return ImmutableHashOrderSet.equals(other,this);
  }

  /** Return the shared table used to hold instances. */
  private static Map<ImmutableHashOrderSet, AbstractCachedSet> table = 
    new ConcurrentHashMap<ImmutableHashOrderSet, AbstractCachedSet>();
  
  @SuppressWarnings("cast")
  @Override
  protected Map<ImmutableHashOrderSet<T>, AbstractCachedSet<T>> getTable() {
    return (Map<ImmutableHashOrderSet<T>, AbstractCachedSet<T>>) (Map) table;
  }

  public static void clearCache() {
    table = new ConcurrentHashMap<ImmutableHashOrderSet, AbstractCachedSet>();
    ++current_generation;
  }

  @Override
  protected AbstractCachedSet<T> newSet(T elements[], boolean inverse) {
    return new CachedSet<T>(elements,inverse);
  }

  private static CachedSet prototype = new CachedSet(new Object[]{},false);
  
  @SuppressWarnings("unchecked")
  public static <T> CachedSet<T> getEmpty() {
    return (CachedSet<T>) prototype.cacheSet(ImmutableHashOrderSet.emptySet());
  }

  @SuppressWarnings("unchecked")
  public static <T> CachedSet<T> getUniverse() {
    return (CachedSet<T>) prototype.cacheSet(ImmutableHashOrderSet.universeSet());
  }
}
