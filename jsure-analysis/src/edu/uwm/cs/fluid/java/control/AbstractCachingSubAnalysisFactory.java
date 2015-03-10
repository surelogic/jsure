/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.uwm.cs.fluid.java.control;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.uwm.cs.fluid.util.Lattice;

/**
 * Sub-analysis factory that always returns the same analysis object for each
 * initializer flow unit, regardless of whether forward or backward analysis is
 * being used, and whether normal or abrupt termination is being analyzed.
 * 
 * <p>
 * This approach won't work if we ever get smart work lists. In this case we
 * will have to create a fresh sub-analysis each time.
 *
 * @param <L>
 *          The type of the lattice that generated flow analyses operate over.
 * @param <T>
 *          The type used to represent the lattice values.
 */
public abstract class AbstractCachingSubAnalysisFactory<L extends Lattice<T>, T>
    implements SubAnalysisFactory<L, T> {
  /**
   * Cached of created subanalyses indexed by the <code>caller</code> 
   * parameter to {@link AbstractCachingSubAnalysisFactory#createAnalysis(IRNode, IBinder, Object, boolean)}.
   */
  private final  Map<IRNode, IJavaFlowAnalysis<T, L>> subAnalyses = new HashMap<IRNode, IJavaFlowAnalysis<T, L>>(); 

  @Override
  public final IJavaFlowAnalysis<T, L> createSubAnalysis(
      final IRNode caller, final IBinder binder, final L lattice,
      final T initialValue, final boolean terminationNormal) {
    IJavaFlowAnalysis<T, L> subAnalysis = subAnalyses.get(caller);
    if (subAnalysis == null) {
      subAnalysis = realCreateAnalysis(
          caller, binder, lattice, initialValue, terminationNormal);
      subAnalyses.put(caller, subAnalysis);
    }
    return subAnalysis;
  }

  /**
   * Get the cached analysis for a given caller.
   * @param caller The caller to get the already created analysis for.
   * @return The subanalysis object or {@value null} if no subanalysis object has been created for the given caller.
   */
  @Override
  public final IJavaFlowAnalysis<T, L> getSubAnalysis(final IRNode caller) {
    return subAnalyses.get(caller);
  }

  /**
   * Delegated to by {@link #createAnalysis(IRNode, IBinder, Object, boolean)}
   * to actually create the subanalysis object. Only called if the subanlaysis
   * object for the given caller has not been previously created. Parameters are
   * as for {@link #createAnalysis(IRNode, IBinder, Object, boolean)}.
   */
  protected abstract IJavaFlowAnalysis<T, L> realCreateAnalysis(
      IRNode caller, IBinder binder,
      L lattice, T initialValue, boolean terminationNormal);
}
