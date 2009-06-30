/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/JavaForwardTransfer.java,v 1.11 2007/03/09 21:54:19 chance Exp $ */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.control.ControlLabel;
import edu.cmu.cs.fluid.control.ForwardTransfer;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.util.Lattice;

/** This abstract class is a skeleton for all Java-specific foward
 * flow-analysis.  It implements the generic transfer routines in terms
 * of Java-specific transfer routines.  Because it inherits most of the
 * work, it needs only define the remaining generic routines.
 */
public abstract class JavaForwardTransfer<T,V> extends JavaTransfer<T,V>
     implements ForwardTransfer<T>
{
  public JavaForwardTransfer(IntraproceduralAnalysis<T,V> base, IBinder binder) {
    super(base, binder);
  }

  /** Transfer when a value is tested for true/false.
   * Needed for ForwardTransfer.
   * @see ForwardTransfer
   */
  public Lattice<T> transferConditional(IRNode node, boolean flag, Lattice<T> before)
  {
    // by default, return the value coming in:
    return before;
  }

  /** Transfer over a test of a label, halting abrupt flow.
   * Needed for ForwardTransfer.
   * @see ForwardTransfer
   */
  public final Lattice<T> transferLabelTest(IRNode node, Object info,
				   ControlLabel label,
				   boolean flag, Lattice<T> before)
  {
    /* In this method, we assume that "info" is a kind of control
     * label that tells us what sort of test we have.
     */
    LabelMatch lm = LabelMatch.compareLabels(label,(ControlLabel)info);
    // System.out.println(label + " " + lm + " " + info);
    if (flag) {
      return (lm.atLeast(LabelMatch.overlaps)) ? before : null;
    } else {
      return (lm.atMost(LabelMatch.includes)) ? before : null;
    }
  }
}
