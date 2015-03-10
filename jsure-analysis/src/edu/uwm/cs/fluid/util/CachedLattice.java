package edu.uwm.cs.fluid.util;

public class CachedLattice<E> extends CachingLattice<E> {
  private final Lattice<E> baseLattice;
 
  /**
   * Create a lattice that caches the results of operations.
   * @param l
   */
  public CachedLattice(Lattice<E> l) {
    baseLattice  = l;
  }
  
  public Lattice<E> getBaseLattice() {
    return baseLattice;
  }

  @Override
  public boolean equals(E v1, E v2) {
    return baseLattice.equals(v1, v2);
  }

  @Override
  public int hashCode(E v) {
    return baseLattice.hashCode(v);
  }

  @Override
  public boolean lessEq(E v1, E v2) {
    return baseLattice.lessEq(v1,v2);
  }
  
  @Override
  protected E computeTop() {
    return baseLattice.top();
  }

  @Override
  protected E computeBottom() {
    return baseLattice.bottom();
  }

  @Override
  protected E computeMeet(E v1, E v2) {
    return baseLattice.meet(v1,v2);
  }

  @Override
  protected E computeJoin(E v1, E v2) {
    return baseLattice.join(v1,v2);
  }

}
