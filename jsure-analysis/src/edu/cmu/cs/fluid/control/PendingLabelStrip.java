/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/PendingLabelStrip.java,v 1.3 2003/07/02 20:19:22 thallora Exp $ */
package edu.cmu.cs.fluid.control;

/** When control-flow passes through this node, the second-most
 * label is removed and checked as a tracking label, and if: <dl>
 * <dt>true<dd> we continue
 * <dt>false<dd> we continue after removing the third label.
 * </dl>
 * This operation is a compound one that could have been written
 * using hypothetical SwapLabel and Droplabel primitives:
 * <pre>
 *				  /-------------------------\
 *   ---SwapLabel---TrackedDemerge			     ---
 *				  \---SwapLabel--DropLabel--/
 * </pre>
 */
public class PendingLabelStrip extends Flow {
  public PendingLabelStrip() { super(); }
}
