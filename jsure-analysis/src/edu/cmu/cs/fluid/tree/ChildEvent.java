/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/ChildEvent.java,v 1.6 2003/07/02 20:19:09 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** Class for events where children of locus are modified.
 * @see NewChildEvent
 * @see ChangedChildEvent
 * @see RemoveChildEvent
 */
public class ChildEvent extends NodeEvent {
  private final IRLocation location;
  private final IRNode childNode;
  public ChildEvent(DigraphInterface dig, IRNode parent, IRLocation loc, IRNode child) {
    super(dig,parent);
    location = loc;
    childNode = child;
  }
  public IRLocation getLocation() { return location; }
  public IRNode getParent() { return getNode(); }
  public IRNode getChild() { return childNode; }
}
