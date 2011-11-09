/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/SinkEvent.java,v 1.4 2003/07/02 20:19:10 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRNode;

/** A class for events where the locus is an edge and
 * the sink of the  edge is changed.
 * @see NewSinkEvent
 * @see ChangedSinkEvent
 */
public class SinkEvent extends EdgeNodeEvent {
  public SinkEvent(DigraphInterface dig, IRNode e, IRNode n) {
    super(dig,e,n);
  }
  public IRNode getSinkNode() { return getNode(); }
}
