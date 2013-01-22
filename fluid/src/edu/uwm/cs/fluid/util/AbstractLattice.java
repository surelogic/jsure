package edu.uwm.cs.fluid.util;

public abstract class AbstractLattice<E> implements Lattice<E> {

  @Override
  public boolean equals(E v1, E v2) {
    return (v1 == null ? v2 == null : v1.equals(v2));
  }

  @Override
  public int hashCode(E v) {
    return v == null ? 0 : v.hashCode();
  }

  @Override
  public E widen(E v1, E v2) {
    return join(v1,v2);
  }
  
  @Override
  public String toString(E v) {
	if (v == null) return "<null>";
    return v.toString();
  }
}
