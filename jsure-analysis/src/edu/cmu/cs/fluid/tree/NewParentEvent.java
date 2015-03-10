/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/NewParentEvent.java,v 1.4 2003/07/02 20:19:10 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

public class NewParentEvent extends ParentEvent {
  public NewParentEvent(DigraphInterface d, IRNode c, IRLocation l, IRNode p) {
    super(d,c,l,p);
  }
}
