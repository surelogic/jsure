/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/ParentEvent.java,v 1.6 2003/07/02 20:19:10 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** Class where event concerns parent of locus.
 * @see NewParentEvent
 * @see ChangedParentEvent
 * @see RemoveParentEvent
 */
public class ParentEvent extends NodeEvent {
  private final IRLocation location;
  private final IRNode parentNode;
  public ParentEvent(DigraphInterface dig, IRNode ch, IRLocation loc, IRNode parent) {
    super(dig,ch);
    location = loc;
    parentNode = parent;
  }
  public IRLocation getLocation() { return location; }
  public IRNode getChild() { return getNode(); }
  public IRNode getParent() { return parentNode; }
}
