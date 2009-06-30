/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/RemoveChildEvent.java,v 1.4 2003/07/02 20:19:11 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

public class RemoveChildEvent extends ChildEvent {
  public RemoveChildEvent(DigraphInterface d, IRNode p, IRLocation l, IRNode c) {
    super(d,p,l,c);
  }
}
