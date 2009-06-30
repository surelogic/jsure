/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/JavaBackwardTransfer.java,v 1.11 2007/03/09 21:54:19 chance Exp $ */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.control.BackwardTransfer;
import edu.cmu.cs.fluid.control.ControlLabel;
import edu.cmu.cs.fluid.java.bind.IBinder;

public abstract class JavaBackwardTransfer<T,V> extends JavaTransfer<T,V>
     implements BackwardTransfer<T> 
{
  public JavaBackwardTransfer(IntraproceduralAnalysis<T,V> base, IBinder binder) {
    super(base, binder);
  }

  /** Transfer backwards over a throw or implicit throw.
   * Needed for BackwardTransfer.
   * @see BackwardTransfer
   */
  public final boolean testAddLabel(ControlLabel matchLabel,
			      ControlLabel label)
  {
    LabelMatch lm = LabelMatch.compareLabels(matchLabel,label);
    return lm.atLeast(LabelMatch.overlaps);
  }
}
