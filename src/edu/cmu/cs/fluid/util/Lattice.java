/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/Lattice.java,v 1.5 2007/03/09 19:58:15 chance Exp $ */
package edu.cmu.cs.fluid.util;

/** An interface to control-flow lattices.
 * In deference to traditional control-flow analyses,
 * the lattice is "upside-down":
 * <dl>
 * <dt>top<dd>    perfect information
 * <dt>bottom<dd> no information
 * <dt>meet<dd>   compute common information
 * </dl>
 * The <tt>equals</tt> method is used to compare elements
 * in the lattice.  
 * <p> The lack of proper binary methods in Java
 * makes this interface too general.  Any type given
 * as <tt>Lattice</tt> should be understood as <tt>ThisType</tt>.
 */
public interface Lattice<T> {
  public Lattice<T> top();
  public Lattice<T> bottom();
  
  /** Your Meet operation (typically either union or intersection, depending on your
   * analysis)
   * @param other The other lattice being merged
   * @return A Lattice representing the result of the meet.  Remember that All Lattices
   * MUST be immutable, so return an all-new lattice if you actually change any values.
   */
  public Lattice<T> meet(Lattice<T> other);
  /** Return true if this information includes the information in the other.
   * @return other.equals(this.meet(other))
   */
  public boolean includes(Lattice<T> other);
}
