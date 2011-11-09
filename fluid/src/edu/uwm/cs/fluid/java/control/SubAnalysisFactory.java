package edu.uwm.cs.fluid.java.control;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.uwm.cs.fluid.util.Lattice;

/**
 * @param <L>
 *          The type of the lattice that generated flow analyses operate over.
 * @param <T>
 *          The type used to represent the lattice values.
 */
public interface SubAnalysisFactory<L extends Lattice<T>, T> {
  /**
   * Called during flow analysis to get a flow analysis object to use to store 
   * the current analysis results about the initializer.
   * 
   * @param caller
   *          The call expression that triggers the need for the subanalysis.
   *          Either a super call in a constructor or an anonymous class
   *          expression.
   * @param binder
   *          The binder to use.
   * @param lattice
   *          The lattice instance to be used by the subanalysis. This should be
   *          the lattice being used by the main analysis.
   * @param initialValue
   *          the value to initialize the subanalysis with.
   * @param terminationNormal
   *          if true then return result of normal termination, otherwise result
   *          of abrupt termination.
   * @return an analysis
   */
  public IJavaFlowAnalysis<T, L> createSubAnalysis(
      IRNode caller, IBinder binder,
      L lattice, T initialValue, boolean terminationNormal);
  
  /**
   * Called after analysis is completed by queries to obtain a flow analysis
   * object that can be used to get the final flow analysis results about the
   * initializer.
   *  
   * @param caller
   *          The call expression that triggers the need for the subanalysis.
   *          Either a super call in a constructor or an anonymous class
   *          expression.
   * @return
   */
  public IJavaFlowAnalysis<T, L> getSubAnalysis(IRNode caller);
}
