/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/SetCache.java,v 1.5 2007/03/09 19:58:15 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.concurrent.ConcurrentHashMap;

/** Class which implements a mapping from sets to cached sets.
 * It also keeps track of the shared empty and universe sets.
 * @see SetLattice
 */
public class SetCache<T> extends ConcurrentHashMap<ImmutableHashOrderSet<T>, AbstractCachedSet<T>> {
  public final SetLattice empty, universe;

  @SuppressWarnings("unchecked")
  public SetCache(SetLattice<T> e) {
    super();
    put(e,e);
    SetLattice<T> u = e.newSet(this,(T[]) new Object[0],true);
    put(u,u);
    empty = e;
    universe = u;
  }
}
