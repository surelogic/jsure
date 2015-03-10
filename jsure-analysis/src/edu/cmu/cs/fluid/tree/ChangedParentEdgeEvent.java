/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/ChangedParentEdgeEvent.java,v 1.5 2006/04/13 21:00:56 boyland Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** A node's incoming edge is changed.
 */
public class ChangedParentEdgeEvent extends ParentEdgeEvent {
  private final IRNode oldEdge;
  public ChangedParentEdgeEvent(DigraphInterface d, IRNode n, IRLocation loc,
				IRNode e0, IRNode e1) {
    super(d,n,loc,e1);
    oldEdge = e0;
  }
  
  public IRNode getOldEdge() {
    return oldEdge;
  }
}
