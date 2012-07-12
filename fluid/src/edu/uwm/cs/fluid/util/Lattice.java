package edu.uwm.cs.fluid.util;

/** A mathematical lattice defined over elements of the type parameter.
 * This class defines the lattice operations; the lattice elements
 * are of the type parameter. <p> This separation of function from data is not
 * object-oriented in the normal sense but makes sense since we often need
 * information stored for all the elements of the lattice (caches, description info etc)
 * that cannot be put in static fields because different lattices will have different
 * values for this information.  A previous design of this class put both together which
 * burdened every instance with having to carry the shared information.  It also made
 * it hard to build a lattice using predefined types. <p>
 * We even have to separate equals and hashcode from the elements since they may
 * be arrays or other objects with the "wrong" hashcodes.
 * <p> The lattice value should never be ``null'' since null is used internally
 * for other purposes.
 * @author boyland
 * @param <E> The base elements of the lattice.  They should be immutable
 */
public interface Lattice<E> extends Hashor<E> {
  /**
   * Return whether the first lattice value is less than
   * or equal to the second.  
   * @param v1 
   * @param v2
   * @return true if v1 &lt;= v2
   */
  public boolean lessEq(E v1, E v2);
  
  /**
   * Return the maximum lattice value.
   * @return maximum lattice value.
   */
  public E top();
  
  /**
   * return the minimum lattice value
   * @return minimum lattice value
   */
  public E bottom();
  
  /**
   * Return the least upper bound of two lattice elements.  (Moves towards top.)
   * @param v1
   * @param v2
   * @return v1 \/ v2
   */
  public E join(E v1, E v2);
  
  /**
   * Return the greatest lower bound of two lattice elements.  (Moves towards bottom.)
   * @param v1
   * @param v2
   * @return v1 /\ v2
   */
  public E meet(E v1, E v2);
  
  /**
   * Perform an upper-bound operation that (one hopes)
   * guarantees that all ascending chains are converted into
   * finite ascending chains.
   * @param v1 a lattice value
   * @param v2 a lattice value that is the result of loop execution
   * @return an upperbound of v1 and v2.
   */
  public E widen(E v1, E v2);
  
  /**
   * Render a lattice value.
   * @param v lattice value to print
   * @return string representation of lattice value
   */
  public String toString(E v);
}
