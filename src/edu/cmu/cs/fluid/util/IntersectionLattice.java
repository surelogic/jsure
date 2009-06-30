/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/IntersectionLattice.java,v 1.9 2007/07/05 18:15:13 aarong Exp $ */
package edu.cmu.cs.fluid.util;


/** A lattice that uses intersections to work down the lattice.
 * Use the public constructor IntersectionLattice() to create
 * a new (separately garbage-collected) lattice.
 */
public final class IntersectionLattice<T> extends SetLattice<T> {
  public IntersectionLattice() {
    super();
  }
  private IntersectionLattice(IntersectionLattice<T> l,
			      T[] elements, boolean inverse)
  {
    super(l,elements,inverse);
  }
  private IntersectionLattice(SetCache<T> c, T[] elements, boolean inverse)
  {
    super(c,elements,inverse);
  }

  @Override
  protected AbstractCachedSet<T> newSet(T[] elements, boolean inverse) {
    return new IntersectionLattice<T>(this,elements,inverse);
  }
  @Override
  protected SetLattice<T> newSet(SetCache<T> c, T[] elements, boolean inverse) {
    return new IntersectionLattice<T>(c,elements,inverse);
  }

  @SuppressWarnings("unchecked")
  public Lattice<T> top() {
    return ((SetCache)getTable()).universe;
  }

  @SuppressWarnings("unchecked")
  public Lattice<T> bottom() {
    return ((SetCache)getTable()).empty;
  }

  public Lattice<T> meet(Lattice<T> other) {
    return (Lattice<T>)intersect((ImmutableHashOrderSet<T>)other);
  }

  public Lattice<T> join(Lattice<T> other) {
    return (Lattice<T>)union((ImmutableHashOrderSet<T>)other);
  }

  public boolean includes(Lattice<T> other) {
    return includes((ImmutableHashOrderSet<T>)other);
  }
}
