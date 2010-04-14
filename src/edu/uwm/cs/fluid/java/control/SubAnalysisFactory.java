package edu.uwm.cs.fluid.java.control;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.uwm.cs.fluid.control.FlowAnalysis;
import edu.uwm.cs.fluid.util.Lattice;

/**
 * @param <L>
 *          The type of the lattice that generated flow analyses operate over.
 * @param <T>
 *          The type used to represent the lattice values.
 */
public interface SubAnalysisFactory<L extends Lattice<T>, T> {
  /**
   * Return a copy of an analysis for use in call initializers.
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
  public FlowAnalysis<T, L> createAnalysis(IRNode caller, IBinder binder,
      L lattice, T initialValue, boolean terminationNormal);
}
