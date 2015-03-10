/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/ChangedParentEvent.java,v 1.4 2003/07/02 20:19:08 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

public class ChangedParentEvent extends ParentEvent {
  private final IRNode oldParent;
  public ChangedParentEvent(DigraphInterface d, IRNode c, IRLocation l,
			    IRNode p0, IRNode p1) {
    super(d,c,l,p1);
    oldParent = p0;
  }
  public IRNode getOldParent() { return oldParent; }
  public IRNode getNewParent() { return getParent(); }
}
