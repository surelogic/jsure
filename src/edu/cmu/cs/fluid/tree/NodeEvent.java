/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/NodeEvent.java,v 1.5 2003/07/02 20:19:08 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRNode;

/** Events for digraphs where the node is the locus.
 * @see NewNodeEvent
 * @see ChildEvent
 * @see ParentEvent
 * @see NodeEdgeEvent
 * @see RemoveChildrenEvent
 * @see RemoveParentsEvent
 */
public class NodeEvent extends DigraphEvent {
  public NodeEvent(DigraphInterface dig, IRNode n) {
    super(dig,n);
  }
  public IRNode getNode() { return getLocus(); }
}
