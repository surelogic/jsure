package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.uwm.cs.fluid.control.FlowAnalysis;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.util.Lattice;

public abstract class AbstractJavaFlowAnalysisQuery<SELF extends AnalysisQuery<R>, R, T, L extends Lattice<T>>
    implements JavaFlowAnalysisQuery<R> {
  protected static enum RawResultFactory {
    ENTRY {
      @Override
      public <T, L extends Lattice<T>, A extends IJavaFlowAnalysis<T, L>> T getRawResult(final IRNode expr, final A analysis, final L lattice) {
        return analysis.getAfter(expr, WhichPort.ENTRY);
      }
    },
    
    NORMAL_EXIT {
      @Override
      public <T, L extends Lattice<T>, A extends IJavaFlowAnalysis<T, L>> T getRawResult(final IRNode expr, final A analysis, final L lattice) {
        return analysis.getAfter(expr, WhichPort.NORMAL_EXIT);
      }
    },
    
    ABRUPT_EXIT {
      @Override
      public <T, L extends Lattice<T>, A extends IJavaFlowAnalysis<T, L>> T getRawResult(final IRNode expr, final A analysis, final L lattice) {
        return analysis.getAfter(expr, WhichPort.ABRUPT_EXIT);
      }
    },
    
    BOTTOM {
      @Override
      public <T, L extends Lattice<T>, A extends IJavaFlowAnalysis<T, L>> T getRawResult(final IRNode expr, final A analysis, final L lattice) {
        return lattice.bottom();
      }
    };
    
    public abstract <T, L extends Lattice<T>, A extends IJavaFlowAnalysis<T, L>> T getRawResult(IRNode expr, A analysis, L lattice);
  }
  
  
  
  /**
   * The flow analysis instance that this query uses to generate results.
   * Usually this is non-null, but if the query is a result of a failed 
   * subanalysis lookup, then this is {@code null}.
   */
  protected final IJavaFlowAnalysis<T, L> analysis;

  /**
   * The lattice object used by the analysis being queried.  We could get
   * this directly from {@link #analysis} using the {@link FlowAnalysis#getLattice()}
   * method, but we choose to cache it here to avoid looking it up all the time.  Also,
   * in the case when the subanalysis lookup fails, {@link #analysis} is
   * {@code null}, so we are provided with the lattice to use directly.
   */
  protected final L lattice;

  /**
   * How to get raw results from the analysis.
   */
  protected final RawResultFactory rawResultFactory;
  
  
  
  /**
   * Create a new normal query, one that is directly associated with a flow
   * analysis.
   */
  protected AbstractJavaFlowAnalysisQuery(final IJavaFlowAnalysis<T, L> a, final RawResultFactory rrf) {
    analysis = a;
    lattice = a.getLattice();
    rawResultFactory = rrf;
  }

  /**
   * Create a new bottom-returning query, one that results from a failed lookup
   * of a subanalysis. These should only occur when the call that should have
   * generated the subanalysis is inside a block that the control flow analysis
   * doesn't visit because its inside obviously dead code, such as
   * 
   * <pre>
   *   if (false} { &hellip; }
   * </pre>
   * 
   * @param l
   *          The lattice to use for the query; this should be the lattice from
   *          the flow analysis object that should have had the subanalysis
   *          object.
   */
  protected AbstractJavaFlowAnalysisQuery(final L l) {
    analysis = null;
    lattice = l;
    rawResultFactory = RawResultFactory.BOTTOM;
  }
  
  public final R getResultFor(final IRNode expr) {
    return processRawResult(expr, rawResultFactory.getRawResult(expr, analysis, lattice));
  }

  public final SELF getSubAnalysisQuery(final IRNode caller) {
    final IJavaFlowAnalysis<T, L> sub =
      analysis.getSubAnalysisFactory().getSubAnalysis(caller);
    if (sub == null) {
      /* We assume the analysis was not created because the code that contains
       * it is dead: it is in an "if (false) { ... }" statement, for example.
       * So the user can query about this code, but the control flow analysis
       * never visits it.  We want to return a query that always returns 
       * BOTTOM.
       * 
       * The problem with this is that I cannot determine if the caller of 
       * this method is just plain confused and shouldn't be asking me about
       * the subanalysis for the given "caller".  Could check for this by
       * checking that caller is a descendant of the flow unit node associated
       * with the current analysis and that no instance initializer blocks
       * come between them, but that is expensive.
       */
      return newBottomReturningSubQuery(lattice);
//      throw new UnsupportedOperationException();
    } else {
      return newAnalysisBasedSubQuery(sub);
    }
  }

  public final boolean hasSubAnalysisQuery(final IRNode caller) {
    return analysis.getSubAnalysisFactory().getSubAnalysis(caller) != null;
  }

  
  
  /**
   * Massage the raw analysis result into a more useful value by running it 
   * through the lattice, etc.
   */
  protected abstract R processRawResult(IRNode expr, T rawResult);
  
  protected abstract SELF newAnalysisBasedSubQuery(IJavaFlowAnalysis<T, L> subAnalysis);

  protected abstract SELF newBottomReturningSubQuery(L lattice);
}
