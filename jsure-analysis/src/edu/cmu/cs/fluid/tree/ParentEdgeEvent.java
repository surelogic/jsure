/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/ParentEdgeEvent.java,v 1.4 2003/07/02 20:19:09 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** Class of events modifying incoming edges of a node.
 * @see NewParentEdgeEvent
 * @see ChangedParentEdgeEvent
 * @see RemoveParentEdgeEvent
 */
public class ParentEdgeEvent extends NodeEdgeEvent {
  public ParentEdgeEvent(DigraphInterface d, IRNode n, IRLocation loc, IRNode e) {
    super(d,n,loc,e);
  }
}
