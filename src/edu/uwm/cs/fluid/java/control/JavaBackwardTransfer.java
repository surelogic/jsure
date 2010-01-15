/* $Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/java/control/JavaBackwardTransfer.java,v 1.4 2007/07/10 22:16:36 aarong Exp $ */
package edu.uwm.cs.fluid.java.control;

import edu.uwm.cs.fluid.control.BackwardTransfer;
import edu.uwm.cs.fluid.control.FlowAnalysis;
import edu.cmu.cs.fluid.control.ControlEdge;
import edu.cmu.cs.fluid.control.ControlLabel;
import edu.cmu.cs.fluid.control.Sink;
import edu.cmu.cs.fluid.control.Source;
import edu.cmu.cs.fluid.java.analysis.LabelMatch;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.uwm.cs.fluid.util.Lattice;

/* Would like to have "A extends BackwardAnalysis<...>", but I cannot do that
 * because BackwardAnalysis is parameterized by the transfer function, and we
 * cannot refer to the self type, JavaBackwardTransfer<...>, in Java.
 */
public abstract class JavaBackwardTransfer<A extends FlowAnalysis<T, L>, L extends Lattice<T>,T> extends JavaTransfer<A, L,T>
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
  
  @Override
  protected final ControlEdge getStartEdge(
      final Source src, final Sink sink) {
    /* Backward transfer starts from the end of the component.  Sink may
     * be either the normal or abrupt output port.
     */
    return sink.getInput();
  }
  
  @Override
  protected final ControlEdge getEndEdge(
      final Source src, final Sink sink) {
    /* Backward transfer ends with the beginning of the component. */
    return src.getOutput();
  }
}
