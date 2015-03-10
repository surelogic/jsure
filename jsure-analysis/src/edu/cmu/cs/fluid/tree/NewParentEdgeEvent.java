/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/NewParentEdgeEvent.java,v 1.4 2003/07/02 20:19:07 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** A node gets a new incoming edge.
 */
public class NewParentEdgeEvent extends ParentEdgeEvent {
  public NewParentEdgeEvent(DigraphInterface d, IRNode n, IRLocation loc, IRNode e) {
    super(d,n,loc,e);
  }
}
