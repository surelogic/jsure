/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/util/OldLattice.java,v 1.2 2007/03/09 20:40:33 chance Exp $*/
package edu.uwm.cs.fluid.util;

import edu.cmu.cs.fluid.util.Lattice;


/**
 * A backward-compatible wrapper for the old combined lattice system.
 * The lattice is also turned upside down.
 * @author boyland
 */
public class OldLattice<T> extends AbstractLattice<Lattice<T>> {
  private final Lattice<T> prototype;
  
  public OldLattice(Lattice<T> l) {
    prototype = l;
  }
  
  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#lessEq(E, E)
   */
  public boolean lessEq(Lattice<T> v1, Lattice<T> v2) {
    return v1.includes(v2);
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#top()
   */
  public Lattice<T> top() {
    return prototype.bottom();
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#bottom()
   */
  public Lattice<T> bottom() {
    return prototype.top();
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#join(E, E)
   */
  public Lattice<T> join(Lattice<T> v1, Lattice<T> v2) {
    return v1.meet(v2);
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#meet(E, E)
   */
  public Lattice<T> meet(Lattice<T> v1, Lattice<T> v2) {
    throw new UnsupportedOperationException("old lattices don't have join");
    // return v1.join(v2);
  }

}
