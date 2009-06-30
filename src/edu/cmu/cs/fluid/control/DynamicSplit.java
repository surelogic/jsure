/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/DynamicSplit.java,v 1.2 2003/07/02 20:19:22 thallora Exp $ */
package edu.cmu.cs.fluid.control;

/** A control flow node that itself can decide whether flow goes
 * to either output.
 */
public abstract class DynamicSplit extends Split {
  public DynamicSplit() { super(); }
  /** Return true if flow can happen between the source
   * and the output indicated.
   * @param flag true if first output, false if second output.
   */
  public abstract boolean test(boolean flag);
}
