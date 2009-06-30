/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/NewChildEdgeEvent.java,v 1.4 2003/07/02 20:19:10 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** A node gets a new outgoing edge.
 */
public class NewChildEdgeEvent extends ChildEdgeEvent {
  public NewChildEdgeEvent(DigraphInterface d, IRNode n, IRLocation loc, IRNode e) {
    super(d,n,loc,e);
  }
}
