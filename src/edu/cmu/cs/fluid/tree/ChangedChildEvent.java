/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/ChangedChildEvent.java,v 1.4 2003/07/02 20:19:07 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

public class ChangedChildEvent extends ChildEvent {
  private final IRNode oldChild;
  public ChangedChildEvent(DigraphInterface d, IRNode p, IRLocation l,
			   IRNode c0, IRNode c1) {
    super(d,p,l,c1);
    oldChild = c0;
  }
  public IRNode getOldChild() { return oldChild; }
  public IRNode getNewChild() { return getChild(); }
}
