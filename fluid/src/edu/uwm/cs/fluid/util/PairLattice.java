/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/util/PairLattice.java,v 1.4 2007/08/22 20:59:03 boyland Exp $*/
package edu.uwm.cs.fluid.util;

import com.surelogic.common.Pair;

/**
 * A lattice built up as the cartesian product of two lattices.
 * @author boyland
 */
public class PairLattice<T1, T2> implements Lattice<Pair<T1, T2>> {
  protected final Lattice<T1> lattice1;
  protected final Lattice<T2> lattice2;
  
  public PairLattice(Lattice<T1> l1, Lattice<T2> l2) {
    lattice1 = l1;
    lattice2 = l2;
  }
  
  public boolean equals(Pair<T1, T2> v1, Pair<T1, T2> v2) {
    return lattice1.equals(v1.first(),v2.first()) && lattice2.equals(v1.second(),v2.second());
  }
  
  public int hashCode(Pair<T1, T2> v) {
    return lattice1.hashCode(v.first()) + lattice2.hashCode(v.second());
  }
  
  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#lessEq(E, E)
   */
  public boolean lessEq(Pair<T1, T2> v1, Pair<T1, T2> v2) {
    return lattice1.lessEq(v1.first(),v2.first()) && lattice2.lessEq(v1.second(),v2.second());
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#top()
   */
  public Pair<T1, T2> top() {
    return new Pair<T1,T2>(lattice1.top(),lattice2.top());
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#bottom()
   */
  public Pair<T1, T2> bottom() {
    return new Pair<T1,T2>(lattice1.bottom(),lattice2.bottom());
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#join(E, E)
   */
  public Pair<T1, T2> join(Pair<T1, T2> v1, Pair<T1, T2> v2) {
    return new Pair<T1,T2>(lattice1.join(v1.first(),v2.first()),
                           lattice2.join(v1.second(),v2.second()));
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#meet(E, E)
   */
  public Pair<T1, T2> meet(Pair<T1, T2> v1, Pair<T1, T2> v2) {
    return new Pair<T1,T2>(lattice1.meet(v1.first(),v2.first()),
                           lattice2.meet(v1.second(),v2.second()));
  }

  public Pair<T1, T2> widen(Pair<T1, T2> v1, Pair<T1, T2> v2) {
    return new Pair<T1,T2>(lattice1.widen(v1.first(),v2.first()),
                                                   lattice2.widen(v1.second(),v2.second()));
  }

  public String toString(Pair<T1,T2> v) {
    return "<" + lattice1.toString(v.first()) + "," + lattice2.toString(v.second()) + ">";
  }
}
