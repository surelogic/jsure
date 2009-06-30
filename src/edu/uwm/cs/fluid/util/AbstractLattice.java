package edu.uwm.cs.fluid.util;

public abstract class AbstractLattice<E> implements Lattice<E> {

  public boolean equals(E v1, E v2) {
    return (v1 == null ? v2 == null : v1.equals(v2));
  }

  public int hashCode(E v) {
    return v == null ? 0 : v.hashCode();
  }

  public E widen(E v1, E v2) {
    return join(v1,v2);
  }
  
  public String toString(E v) {
    return v.toString();
  }
}
