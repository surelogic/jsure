/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/NewNodeEvent.java,v 1.5 2003/07/02 20:19:09 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRNode;

public class NewNodeEvent extends NodeEvent {
  public NewNodeEvent(DigraphInterface dig, IRNode n) {
    super(dig,n);
  }
}
