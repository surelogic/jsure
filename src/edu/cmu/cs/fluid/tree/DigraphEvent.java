/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/DigraphEvent.java,v 1.5 2003/07/02 20:19:07 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import java.util.EventObject;

import edu.cmu.cs.fluid.ir.IRNode;

/** Abstract class for events for directed graphs.
 * @see NodeEvent
 * @see EdgeEvent
 */
public abstract class DigraphEvent extends EventObject {
  private final DigraphInterface digraph;
  private final IRNode locus;
  public DigraphEvent(DigraphInterface dig, IRNode n) {
    super(dig);
    digraph = dig;
    locus = n;
  }
  public DigraphInterface getDigraph() { return digraph; }
  public IRNode getLocus() { return locus; }
}
