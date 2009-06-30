/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/util/LatticeWrapper.java,v 1.1 2006/04/28 21:42:49 boyland Exp $*/
package edu.uwm.cs.fluid.util;


/**
 * A basic class to help writing lattice decorators.
 * @author boyland
 */
public class LatticeWrapper<T> implements Lattice<T> {
  private final Lattice<T> wrapped;
  
  public LatticeWrapper(Lattice<T> l) {
    wrapped = l;
  }
  
  public boolean equals(T v1, T v2) {
    return wrapped.equals(v1,v2);
  }

  public int hashCode(T v) {
    return wrapped.hashCode(v);
  }

  public String toString(T v) {
    return wrapped.toString(v);
  }

  public T widen(T v1, T v2) {
    return wrapped.widen(v1,v2);
  }

  public T bottom() {
    return wrapped.bottom();
  }

  public T join(T v1, T v2) {
    return wrapped.join(v1,v2);
  }

  public boolean lessEq(T v1, T v2) {
    return wrapped.lessEq(v1,v2);
  }

  public T meet(T v1, T v2) {
    return wrapped.meet(v1,v2);
  }

  public T top() {
    return wrapped.top();
  }
}
