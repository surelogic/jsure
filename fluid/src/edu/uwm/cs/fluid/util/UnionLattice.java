/**
 * 
 */
package edu.uwm.cs.fluid.util;

import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableSet;

/**
 * @author boyland
 *
 */
public class UnionLattice<T> extends AbstractLattice<ImmutableSet<T>> {

  public boolean lessEq(ImmutableSet<T> v1, ImmutableSet<T> v2) {
    return v2.containsAll(v1);
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#equals(E, E)
   */
  @Override
  public boolean equals(ImmutableSet<T> v1, ImmutableSet<T> v2) {
    return v1.equals(v2);
  }

  public ImmutableSet<T> top() {
    // needs unchecked conversion, but Ok since you don't ever
    // iterate over the elements.
    return ImmutableHashOrderSet.universeSet();
  }

  public ImmutableSet<T> bottom() {
    // needs unchecked conversion, but OK since it doesn't have any elements in it.
    return ImmutableHashOrderSet.emptySet();
  }

  public ImmutableSet<T> join(ImmutableSet<T> v1, ImmutableSet<T> v2) {
    if (v2 == null) {
      throw new NullPointerException("null is not a legal set");
    }
    return v1.union(v2);
  }

  public ImmutableSet<T> meet(ImmutableSet<T> v1, ImmutableSet<T> v2) {
    return v1.intersection(v2);
  }

  
}
