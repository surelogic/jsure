/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/ChangedSourceEvent.java,v 1.4 2003/07/02 20:19:07 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRNode;

/** A class for events where the locus is an edge and
 * the source of the edge is changed.
 */
public class ChangedSourceEvent extends SourceEvent {
  private final IRNode oldSource;
  public ChangedSourceEvent(DigraphInterface dig, IRNode e, IRNode n0, IRNode n1) {
    super(dig,e,n1);
    oldSource = n0;
  }
  public IRNode getOldSource() {
    return oldSource;
  }
}
