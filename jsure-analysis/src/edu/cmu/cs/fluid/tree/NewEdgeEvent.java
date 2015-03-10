/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/NewEdgeEvent.java,v 1.4 2003/07/02 20:19:08 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRNode;

public class NewEdgeEvent extends EdgeEvent {
  public NewEdgeEvent(DigraphInterface dig, IRNode e) {
    super(dig,e);
  }
}
