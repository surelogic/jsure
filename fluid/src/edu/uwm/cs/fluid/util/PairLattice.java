package edu.uwm.cs.fluid.util;

import com.surelogic.common.Pair;

/**
 * A lattice built up as the Cartesian product of two lattices.
 */
public abstract class PairLattice<T1, T2, L1 extends Lattice<T1>, L2 extends Lattice<T2>, V extends Pair<T1, T2>> implements Lattice<V> {
  protected final L1 lattice1;
  protected final L2 lattice2;
  
  public PairLattice(final L1 l1, final L2 l2) {
    lattice1 = l1;
    lattice2 = l2;
  }
  
  protected abstract V newPair(T1 v1, T2 v2);
  
  
  
  @Override
  public final boolean equals(final V v1, final V v2) {
    return lattice1.equals(v1.first(), v2.first()) 
        && lattice2.equals(v1.second(), v2.second());
  }
  
  @Override
  public final int hashCode(final V v) {
    return lattice1.hashCode(v.first()) + lattice2.hashCode(v.second());
  }

  @Override
  public final boolean lessEq(final V v1, final V v2) {
    return lattice1.lessEq(v1.first(), v2.first())
        && lattice2.lessEq(v1.second(), v2.second());
  }

  @Override
  public final V top() {
    return newPair(lattice1.top(), lattice2.top());
  }

  @Override
  public final V bottom() {
    return newPair(lattice1.bottom(), lattice2.bottom());
  }

  @Override
  public final V join(final V v1, final V v2) {
    return newPair(lattice1.join(v1.first(), v2.first()),
        lattice2.join(v1.second(), v2.second()));
  }

  @Override
  public final V meet(final V v1, final V v2) {
    return newPair(lattice1.meet(v1.first(), v2.first()),
        lattice2.meet(v1.second(), v2.second()));
  }

  @Override
  public final V widen(final V v1, final V v2) {
    return newPair(lattice1.widen(v1.first(), v2.first()),
        lattice2.widen(v1.second(), v2.second()));
  }

  @Override
  public String toString(final V v) {
    return "<" + lattice1.toString(v.first()) + "," + lattice2.toString(v.second()) + ">";
  }
}
