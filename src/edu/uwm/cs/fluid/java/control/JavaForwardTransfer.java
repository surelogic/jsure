/* $Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/java/control/JavaForwardTransfer.java,v 1.4 2007/07/10 22:16:36 aarong Exp $ */
package edu.uwm.cs.fluid.java.control;

import edu.cmu.cs.fluid.control.ControlLabel;
import edu.uwm.cs.fluid.control.ForwardTransfer;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.LabelMatch;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.uwm.cs.fluid.util.Lattice;

/** This abstract class is a skeleton for all Java-specific foward
 * flow-analysis.  It implements the generic transfer routines in terms
 * of Java-specific transfer routines.  Because it inherits most of the
 * work, it needs only define the remaining generic routines.
 */
public abstract class JavaForwardTransfer<L extends Lattice<T>,T> extends JavaTransfer<L,T>
     implements ForwardTransfer<T>
{
  public JavaForwardTransfer(IBinder binder, L lattice) {
    super(binder,lattice);
  }

  /** Transfer when a value is tested for true/false.
   * Needed for ForwardTransfer.
   * @see ForwardTransfer
   */
  public T transferConditional(IRNode node, boolean flag, T before)
  {
    // by default, return the value coming in:
    return before;
  }

  /** Transfer over a test of a label, halting abrupt flow.
   * Needed for ForwardTransfer.
   * @see ForwardTransfer
   */
  public final boolean transferLabelTest(IRNode node, Object info,
				   ControlLabel label,
				   boolean flag)
  {
    /* In this method, we assume that "info" is a kind of control
     * label that tells us what sort of test we have.
     */
    LabelMatch lm = LabelMatch.compareLabels(label,(ControlLabel)info);
    // System.out.println(label + " " + lm + " " + info);
    if (flag) {
      return lm.atLeast(LabelMatch.overlaps);
    } else {
      return lm.atMost(LabelMatch.includes);
    }
  }

  public T transferLoopMerge(IRNode node, T initial, T looped) {
    return lattice.widen(initial,looped);
  }
}
