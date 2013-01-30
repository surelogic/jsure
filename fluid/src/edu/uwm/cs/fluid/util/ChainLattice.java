/* $Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/util/ChainLattice.java,v 1.1 2006/03/10 04:14:42 boyland Exp $ */
package edu.uwm.cs.fluid.util;


/**
 * The chain lattice.  A lattice that consists simple of a chain:
 * top > ... > bottom = 0.
 * @author boyland
 */
public class ChainLattice extends AbstractLattice<Integer> {
  private final Integer max;
  private static final Integer zero = new Integer(0);
  
  /**
   * Create a chain lattice where top = maximum and 0 = bottom
   * @param maximum
   */
  public ChainLattice(int maximum) {
    max = maximum;
  }
  
  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#lessEq(E, E)
   */
  @Override
  public boolean lessEq(Integer v1, Integer v2) {
    return v1 <= v2;
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#top()
   */
  @Override
  public Integer top() {
    return max;
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#bottom()
   */
  @Override
  public Integer bottom() {
    return zero;
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#join(E, E)
   */
  @Override
  public Integer join(Integer v1, Integer v2) {
    // max
    return v1 < v2 ? v2 : v1;
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.util.Lattice#meet(E, E)
   */
  @Override
  public Integer meet(Integer v1, Integer v2) {
    // min
    return v1 < v2 ? v1 : v2;
  }

}
