/* $Header$ */
package edu.cmu.cs.fluid.control;

import java.util.NoSuchElementException;

/** Class to represent a single node as an enumeration.
 * @author John Tang Boyland
 */
class SingleControlEdgeIterator extends ControlEdgeIterator {
  private ControlEdge next;
  private boolean returned = false;
  /** Create an enumeration of control nodes.
   * @param elem the node to be returned (null if enumeration is to be empty
   */
  SingleControlEdgeIterator (ControlEdge elem) {
    this.next = elem;
  }
  @Override
  public boolean hasNext() { return !returned; }
  @Override
  public ControlEdge nextControlEdge() throws NoSuchElementException {
    if (!returned) {
      returned = true;
      return next;
    } else {
      throw new NoSuchElementException();
    }
  }
}
