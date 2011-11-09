/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/RemoveParentEdgeEvent.java,v 1.3 2003/07/02 20:19:10 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

public class RemoveParentEdgeEvent extends ParentEdgeEvent {
  public RemoveParentEdgeEvent(DigraphInterface d, IRNode n, IRLocation l, IRNode e) {
    super(d,n,l,e);
  }
}
