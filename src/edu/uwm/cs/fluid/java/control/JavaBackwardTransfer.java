/* $Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/java/control/JavaBackwardTransfer.java,v 1.4 2007/07/10 22:16:36 aarong Exp $ */
package edu.uwm.cs.fluid.java.control;

import edu.uwm.cs.fluid.control.BackwardTransfer;
import edu.cmu.cs.fluid.control.ControlLabel;
import edu.cmu.cs.fluid.java.analysis.LabelMatch;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.uwm.cs.fluid.util.Lattice;

public abstract class JavaBackwardTransfer<L extends Lattice<T>,T> extends JavaTransfer<L,T>
     implements BackwardTransfer<T>
{
  public JavaBackwardTransfer(IBinder binder, L lattice) {
    super(binder,lattice);
  }

  /** Transfer backwards over a throw or implicit throw.
   * Needed for BackwardTransfer.
   * @see BackwardTransfer
   */
  public final boolean testAddLabel(ControlLabel matchLabel,ControlLabel label)
  {
    LabelMatch lm = LabelMatch.compareLabels(matchLabel,label);
    return lm.atLeast(LabelMatch.overlaps);
  }
}
