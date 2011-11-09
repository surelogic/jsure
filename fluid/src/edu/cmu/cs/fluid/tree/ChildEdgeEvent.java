/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/ChildEdgeEvent.java,v 1.4 2003/07/02 20:19:11 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** Class of events modifying outgoing edges of a node.
 * @see NewChildEdgeEvent
 * @see ChangedChildEdgeEvent
 * @see RemoveChildEdgeEvent
 */
public class ChildEdgeEvent extends NodeEdgeEvent {
  public ChildEdgeEvent(DigraphInterface d, IRNode n, IRLocation loc, IRNode e) {
    super(d,n,loc,e);
  }
}
