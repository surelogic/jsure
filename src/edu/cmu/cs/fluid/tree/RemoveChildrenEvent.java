/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/RemoveChildrenEvent.java,v 1.4 2003/07/02 20:19:10 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRNode;

/** Children of node have all been removed.
 */
public class RemoveChildrenEvent extends NodeEvent {
  public RemoveChildrenEvent(DigraphInterface dig, IRNode parent) {
    super(dig,parent);
  }
  public IRNode getParent() { return getNode(); }
}
