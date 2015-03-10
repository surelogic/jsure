/**
 * 
 */
package edu.uwm.cs.fluid.util;

import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableSet;

/**
 * @author boyland
 */
public class IntersectionLattice<T> extends AbstractLattice<ImmutableSet<T>> {

  @Override
  public boolean lessEq(ImmutableSet<T> v1, ImmutableSet<T> v2) {
    return v1.containsAll(v2);
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#equals(E, E)
   */
  @Override
  public boolean equals(ImmutableSet<T> v1, ImmutableSet<T> v2) {
    return v1.equals(v2);
  }

  @Override
  public ImmutableSet<T> bottom() {
    // needs unchecked conversion, but Ok since you don't ever
    // iterate over the elements.
    return ImmutableHashOrderSet.universeSet();
  }

  @Override
  public ImmutableSet<T> top() {
    // needs unchecked conversion, but OK since it doesn't have any elements in it.
    return ImmutableHashOrderSet.emptySet();
  }

  @Override
  public ImmutableSet<T> meet(ImmutableSet<T> v1, ImmutableSet<T> v2) {
    return v1.union(v2);
  }

  @Override
  public ImmutableSet<T> join(ImmutableSet<T> v1, ImmutableSet<T> v2) {
    return v1.intersection(v2);
  }
}
