/* $Header$ */
package edu.cmu.cs.fluid.control;

import java.util.NoSuchElementException;

/** A class for empty enumerations of control-flow edges. */
class EmptyControlEdgeIterator extends ControlEdgeIterator {
  public static EmptyControlEdgeIterator prototype = 
      new EmptyControlEdgeIterator();
  @Override
  public boolean hasNext() {
    return false;
  }
  @Override
  public ControlEdge nextControlEdge() 
      throws NoSuchElementException
  {
    throw new NoSuchElementException("enumeration is empty");
  }
}
