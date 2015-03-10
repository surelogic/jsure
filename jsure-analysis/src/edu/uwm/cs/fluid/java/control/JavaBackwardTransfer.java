package edu.uwm.cs.fluid.java.control;

import edu.uwm.cs.fluid.control.BackwardTransfer;
import edu.cmu.cs.fluid.control.ControlEdge;
import edu.cmu.cs.fluid.control.ControlLabel;
import edu.cmu.cs.fluid.control.Sink;
import edu.cmu.cs.fluid.control.Source;
import edu.cmu.cs.fluid.java.analysis.LabelMatch;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.uwm.cs.fluid.util.Lattice;

public abstract class JavaBackwardTransfer<L extends Lattice<T>, T> extends JavaTransfer<L, T>
     implements BackwardTransfer<T>
{
  public JavaBackwardTransfer(
      final IBinder binder, final L lattice, final SubAnalysisFactory<L, T> factory) {
    super(binder,lattice, factory);
  }

  /** Transfer backwards over a throw or implicit throw.
   * Needed for BackwardTransfer.
   * @see BackwardTransfer
   */
  @Override
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
