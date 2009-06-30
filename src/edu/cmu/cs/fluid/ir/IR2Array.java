/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IR2Array.java,v 1.4 2006/03/31 21:27:20 chance Exp $
 */
package edu.cmu.cs.fluid.ir;

/**
 * A specialized version of {@link IRArray} for fixed-size sequences of
 * exactly two elements.
 * @author boyland
 */
public abstract class IR2Array<S,T> extends IRAbstractArray<S,T> {
  S elem1, elem2;
  
  public IR2Array() {
    initialize();
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRSequence#size()
   */
  public int size() {
    return 2;
  }

  @Override
  protected S getInternal(int i) {
    if (i == 0) return elem1;
    return elem2;
  }
  
  @Override
  protected void setInternal(int i, S slotState) {
    if (i == 0) elem1 = slotState;
    else elem2 = slotState;
  }
}
