/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/NewSourceEvent.java,v 1.4 2003/07/02 20:19:08 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRNode;

/** A class for events where the locus is an edge and
 * the source of the edge is defined.
 */
public class NewSourceEvent extends SourceEvent {
  public NewSourceEvent(DigraphInterface dig, IRNode e, IRNode n) {
    super(dig,e,n);
  }
}
