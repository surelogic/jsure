/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/util/PairLattice.java,v 1.4 2007/08/22 20:59:03 boyland Exp $*/
package edu.uwm.cs.fluid.util;

import edu.cmu.cs.fluid.util.Triple;

/**
 * A lattice built up as the cartesian product of three lattices.
 */
public abstract class TripleLattice<T1, T2, T3, V extends Triple<T1, T2, T3>> implements Lattice<V> {
  /* Our constituent lattices are public!  Not usually needed, but sometimes
   * useful.  I cannot think of a good reason they should be hidden.
   */
  public final Lattice<T1> lattice1;
  public final Lattice<T2> lattice2;
  public final Lattice<T3> lattice3;
  
  private final V top;
  private final V bottom;
  
  
  
  public TripleLattice(
      final Lattice<T1> l1, final Lattice<T2> l2, final Lattice<T3> l3) {
    lattice1 = l1;
    lattice2 = l2;
    lattice3 = l3;
    top = newTriple(
        lattice1.top(), lattice2.top(), lattice3.top());
    bottom = newTriple(
        lattice1.bottom(), lattice2.bottom(), lattice3.bottom());
  }
  
  protected abstract V newTriple(T1 a, T2 b, T3 c);
  
  
  
  public final boolean equals(
      final V v1, final V v2) {
    return lattice1.equals(v1.first(), v2.first()) &&
      lattice2.equals(v1.second(), v2.second()) &&
      lattice3.equals(v1.third(), v2.third());
  }
  
  public final int hashCode(final V v) {
    return lattice1.hashCode(v.first()) +
      lattice2.hashCode(v.second()) +
      lattice3.hashCode(v.third());
  }
  
  public final boolean lessEq(V v1, V v2) {
    return lattice1.lessEq(v1.first(), v2.first()) &&
    lattice2.lessEq(v1.second(), v2.second()) &&
    lattice3.lessEq(v1.third(), v2.third());
  }

  public final V top() {
    return top;
  }

  public final V bottom() {
    return bottom;
  }

  public V join(
      final V v1, final V v2) {
    return newTriple(
        lattice1.join(v1.first(), v2.first()),
        lattice2.join(v1.second(), v2.second()),
        lattice3.join(v1.third(), v2.third()));
  }

  public V meet(
      final V v1, final V v2) {
    return newTriple(
        lattice1.meet(v1.first(),v2.first()),
        lattice2.meet(v1.second(),v2.second()),
        lattice3.meet(v1.third(), v2.third()));
  }

  public V widen(
      final V v1, final V v2) {
    return newTriple(
        lattice1.widen(v1.first(), v2.first()),
        lattice2.widen(v1.second(), v2.second()),
        lattice3.widen(v1.third(), v2.third()));
  }

  public String toString(final V v) {
    return "<" + lattice1.toString(v.first()) + "," + 
      lattice2.toString(v.second()) + "," +
      lattice3.toString(v.third()) + ">";
  }
  
  
  
  protected V canonicalize(final V newV, final V oldV) {
    // Inspired by the checks in the old RecordLattice.replaceValues()
    if (top.equals(newV)) return top;
    if (bottom.equals(newV)) return bottom;
    if (oldV.equals(newV)) return oldV;
    return newV;
  }
  
  public final V replaceFirst(final V v, final T1 first) {
    return canonicalize(newTriple(first, v.second(), v.third()), v);
  }
  
  public final V replaceSecond(final V v, final T2 second) {
    return canonicalize(newTriple(v.first(), second, v.third()), v);
  }
  
  public final V replaceThird(final V v, final T3 third) {
    return canonicalize(newTriple(v.first(), v.second(), third), v);
  }
}
