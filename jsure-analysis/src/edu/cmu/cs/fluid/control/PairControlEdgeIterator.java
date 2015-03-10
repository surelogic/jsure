/* $Header$ */
package edu.cmu.cs.fluid.control;

import java.util.NoSuchElementException;

/** Class to represent an enumeration that has up to two nodes.
 * @author John Tang Boyland
 */
class PairControlEdgeIterator extends ControlEdgeIterator {
  private ControlEdge next1, next2;
  private int returned = 0;
  /** Create an enumeration of control nodes.
   * @param elem1 the node to be returned first
   * @param elem2 the node to be returned next
   */
  PairControlEdgeIterator (ControlEdge elem1, ControlEdge elem2) {
    this.next1 = elem1;
    this.next2 = elem2;
  }
  @Override
  public boolean hasNext() { 
    return returned < 2;
  }
  @Override
  public ControlEdge nextControlEdge() throws NoSuchElementException {
    switch (returned) {
    case 0:
      returned = 1;
      return next1;
    case 1:
      returned = 2;
      return next2;
    default:
      throw new NoSuchElementException();
    }
  }
}
