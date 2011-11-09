/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/AbstractCachedSet.java,v 1.11 2007/10/10 02:09:12 boyland Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.*;

/** A cached set is only allocated once for each array of elements.
 * This way we can use == to test set equality.  It also saves
 * memory usage.  To save memory, the table used can be changed.
 * Either a static table can be declard (in a subclass) which is
 * periodically left to float away, or else a new instance of empty
 * and universe can be declared with a shared new table.
 * (Better would be a static "weak" hashtable.)
 */
@SuppressWarnings("unchecked")
public abstract class AbstractCachedSet<T> extends ImmutableHashOrderSet<T> {
  protected AbstractCachedSet(T[] elements, boolean inverse) {
    super(elements,inverse);
  }

  /** Return the shared/thread-safe table used to hold instances. */
  protected abstract Map<ImmutableHashOrderSet<T>, AbstractCachedSet<T>> getTable();

  public int cacheSize() { return getTable().size(); }

  protected abstract AbstractCachedSet<T>
      newSet(T[] elements, boolean inverse);

  @SuppressWarnings("unchecked")
  public AbstractCachedSet<T> cacheSet(ImmutableHashOrderSet<T> s) {
    Map<ImmutableHashOrderSet<T>, AbstractCachedSet<T>> t = getTable();
    AbstractCachedSet<T> c = t.get(s);
    if (c == null) {
      c = newSet(s.elements,s.inverse);
      t.put(c,c);
    }
    return c;
  }

  /* we simulate parasitic methods */
  @Override
  public boolean equals(ImmutableHashOrderSet other) {
    if (other instanceof AbstractCachedSet) {
      return equals((AbstractCachedSet)other);
    } else {
      return super.equals(other);
    }
  }
  public boolean equals(AbstractCachedSet other) {
    if (getTable() == other.getTable()) {
      return this == other;
    } else {
      return super.equals(other);
    }
  }

  // Now we redefine all the methods that create new sets to cache the results.
  //! Java does not permit the return type to be changed in overriding methods
  @Override
  public AbstractCachedSet<T> invert() {
    return cacheSet(super.invert());
  }
  @Override
  public AbstractCachedSet<T> addElement(T elem) {
    return cacheSet(super.addElement(elem));
  }
  @Override
  public AbstractCachedSet<T> removeElement(T elem) {
    return cacheSet(super.removeElement(elem));
  }
	@Override
  public AbstractCachedSet<T> addElements(Iterator<T> it) {
		return cacheSet(super.addElements(it));
	}  
  @Override
  public AbstractCachedSet<T> union(ImmutableHashOrderSet<T> other) {
    return cacheSet(super.union(other));
  }
  @Override
  public AbstractCachedSet<T> intersect(ImmutableHashOrderSet<T> other) {
    return cacheSet(super.intersect(other));
  }
  @Override
  public AbstractCachedSet<T> difference(ImmutableHashOrderSet<T> other) {
    return cacheSet(super.difference(other));
  }

  // useful methods:
  public AbstractCachedSet<T> empty() { return cacheSet(ImmutableHashOrderSet.<T>emptySet()); }
  public AbstractCachedSet<T> universe() { return cacheSet(ImmutableHashOrderSet.<T>universeSet()); }
}
