package edu.uwm.cs.fluid.java.control;

import edu.cmu.cs.fluid.ir.IRNodeViewer;
import edu.uwm.cs.fluid.control.ForwardAnalysis;
import edu.uwm.cs.fluid.util.Lattice;

public class JavaForwardAnalysis<T, L extends Lattice<T>>
    extends ForwardAnalysis<T, L, JavaForwardTransfer<L, T>> implements IJavaFlowAnalysis<T, L> {
  public JavaForwardAnalysis(final String name, final L l, final JavaForwardTransfer<L, T> t, final IRNodeViewer nv) {
    super(name, l, t, nv);
  }
  
  public JavaForwardAnalysis(final String name, final L l, final JavaForwardTransfer<L, T> t, final IRNodeViewer nv, final boolean timeOut) {
    super(name, l, t, nv, timeOut);
  }
  
  
  /**
   * Expose the sub analysis factory so that queries can access the sub analysis
   * objects.  Cannot have a {@code getSubAnalysis()} method directly because
   * that would require knowing the actual type of the returned analysis, which
   * creates a cyclic dependency in the type parameters of the FlowAnalysis and 
   * JavaTransfer implementations.  This way only the actual type passed to
   * {@code SAF} needs to the actual type of the flow analysis implementation.
   * 
   */
  @Override
  public SubAnalysisFactory<L, T> getSubAnalysisFactory() {
    return trans.getSubAnalysisFactory();
  }
}
