/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/SetLattice.java,v 1.9 2007/10/10 02:09:12 boyland Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.Hashtable;

/** Abstract class for lattices over sets.
 * It is implemented using a cached set type, and a new cache
 * is created every time a SetLattice() is created, thus allowing
 * the cache to be garbage collected when none of the elements
 * of the lattice are referenced any more.
 * @see UnionLattice
 * @see IntersectionLattice
 */
public abstract class SetLattice<T> extends AbstractCachedSet<T> implements Lattice<T> {
  private final SetCache<T> cache;

  @SuppressWarnings("unchecked")
  public SetLattice() {
    super((T[]) new Object[0],false);
    cache = createCache();
  }
  protected SetCache<T> createCache() {
    return new SetCache<T>(this);
  }
    
  /** Constructor to be called by newSet(Object[],boolean) */
  protected SetLattice(SetLattice<T> old, T[] elements, boolean inverse) {
    super(elements,inverse);
    cache = old.cache;
  }

  /** Constructor to be called by newSet(SetCache,Object[],boolean) */
  protected SetLattice(SetCache<T> c, T[] elements, boolean inverse) {
    super(elements,inverse);
    cache = c;
  }
  protected abstract SetLattice<T> newSet(SetCache<T> cache,
				       T[] elements,
				       boolean inverse);

  @Override
  protected Hashtable<ImmutableHashOrderSet<T>, AbstractCachedSet<T>> getTable() {
    return (Hashtable<ImmutableHashOrderSet<T>, AbstractCachedSet<T>>) cache;
  }
}
