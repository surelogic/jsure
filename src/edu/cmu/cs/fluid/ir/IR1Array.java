/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IR1Array.java,v 1.5 2006/03/31 21:27:20 chance Exp $
 */
package edu.cmu.cs.fluid.ir;

/**
 * A generic fixed size single-element IR sequence.
 * @author boyland
 */
public abstract class IR1Array<S,T> extends IRAbstractArray<S,T> {
  protected S element;
  
  /**
   * Create an array of one element.
   */
  public IR1Array() {
    super();
    initialize();
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRSequence#size()
   */
  public int size() {
    return 1;
  }

  @Override
  protected S getInternal(int i) {
    return element;
  }
  @Override
  protected void setInternal(int i, S slotState) {
    element = slotState;
  }
}
