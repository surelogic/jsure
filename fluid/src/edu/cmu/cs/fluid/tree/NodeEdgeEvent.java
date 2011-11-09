/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/NodeEdgeEvent.java,v 1.4 2003/07/02 20:19:10 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** Class for events where edges of locus are modified.
 * @see ChildEdgeEvent
 * @see ParentEdgeEvent
 */
public class NodeEdgeEvent extends NodeEvent {
  private final IRLocation location;
  private final IRNode edgeNode;
  public NodeEdgeEvent(DigraphInterface dig, IRNode node, IRLocation loc, IRNode edge) {
    super(dig,node);
    location = loc;
    edgeNode = edge;
  }
  public IRLocation getLocation() { return location; }
  public IRNode getEdge() { return edgeNode; }
}
