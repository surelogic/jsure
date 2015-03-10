/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/ChangedSinkEvent.java,v 1.4 2003/07/02 20:19:08 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRNode;

/** A class for events where the locus is an edge and
 * the sink of the edge is changed.
 */
public class ChangedSinkEvent extends SinkEvent {
  private final IRNode oldSink;
  public ChangedSinkEvent(DigraphInterface dig, IRNode e, IRNode n0, IRNode n1)
  {
    super(dig,e,n1);
    oldSink = n0;
  }
  public IRNode getOldSink() {
    return oldSink;
  }
}
