/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/EdgeNodeEvent.java,v 1.4 2003/07/02 20:19:08 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRNode;

/** A class for events where the locus is an edge and
 * the source or sink of the  edge is changed.
 */
public class EdgeNodeEvent extends EdgeEvent {
  private IRNode node;
  public EdgeNodeEvent(DigraphInterface dig, IRNode e, IRNode n) {
    super(dig,e);
    node = n;
  }
  public IRNode getNode() {
    return node;
  }
}
