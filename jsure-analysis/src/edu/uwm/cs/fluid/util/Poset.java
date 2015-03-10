package edu.uwm.cs.fluid.util;

/** 
 * A partially ordered set (POSET).
 * The poset value should never be ``null'' because null is used internally
 * for other purposes.
 * 
 * @param <E> The base elements of the poset.  They should be immutable.
 */
public interface Poset<E> {
  /**
   * Return whether the first value is less than
   * or equal to the second.  
   * @param v1 
   * @param v2
   * @return true if v1 &lt;= v2
   */
  public boolean lessEq(E v1, E v2);
}
