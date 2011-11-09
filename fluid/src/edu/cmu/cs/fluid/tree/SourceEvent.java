/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/SourceEvent.java,v 1.4 2003/07/02 20:19:11 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRNode;

/** A class for events where the locus is an edge and
 * the source of the  edge is changed.
 * @see NewSourceEvent
 * @see ChangedSourceEvent
 */
public class SourceEvent extends EdgeNodeEvent {
  public SourceEvent(DigraphInterface dig, IRNode e, IRNode n) {
    super(dig,e,n);
  }
  public IRNode getSourceNode() { return getNode(); }
}
