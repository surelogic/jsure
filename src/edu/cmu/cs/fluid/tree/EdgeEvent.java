/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/EdgeEvent.java,v 1.4 2003/07/02 20:19:10 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRNode;

/** Events for digraphs where the edge is the locus.
 * @see NewEdgeEvent
 * @see EdgeNodeEvent
 */
public class EdgeEvent extends DigraphEvent {
  public EdgeEvent(DigraphInterface dig, IRNode n) {
    super(dig,n);
  }
  public IRNode getEdge() { return getLocus(); }
}
