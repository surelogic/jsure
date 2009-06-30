/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/UnionLattice.java,v 1.9 2007/03/09 19:58:15 chance Exp $ */
package edu.cmu.cs.fluid.util;


/** A lattice that uses unions to work down the lattice.
 * To create a new (separately garbage-collectable) lattice,
 * use the public constructor UnionLattice.
 */
public final class UnionLattice<T> extends SetLattice<T> {
  public UnionLattice() {
    super();
  }
  private UnionLattice(UnionLattice<T> l, T[] elements, boolean inverse) {
    super(l,elements,inverse);
  }
  private UnionLattice(SetCache<T> c, T[] elements, boolean inverse) {
    super(c,elements,inverse);
  }

  @Override
  protected AbstractCachedSet<T> newSet(T[] elements, boolean inverse) {
    return new UnionLattice<T>(this,elements,inverse);
  }
  @Override
  protected SetLattice<T> newSet(SetCache<T> c, T[] elements, boolean inverse) {
    return new UnionLattice<T>(c,elements,inverse);
  }

  @SuppressWarnings("unchecked")
  public Lattice<T> top() {
    return ((SetCache)getTable()).empty;
  }

  @SuppressWarnings("unchecked")
  public Lattice<T> bottom() {
    return ((SetCache)getTable()).universe;
  }

  public Lattice<T> meet(Lattice<T> other) {
    return (Lattice<T>)union((ImmutableHashOrderSet<T>)other);
  }

  public Lattice<T> join(Lattice<T> other) {
    return (Lattice<T>)intersect((ImmutableHashOrderSet<T>)other);
  }

  public boolean includes(Lattice<T> other) {
    return ((ImmutableHashOrderSet)other).includes(this);
  }
}
