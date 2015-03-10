package edu.cmu.cs.fluid.java.analysis;

import com.surelogic.util.IThunk;

import edu.cmu.cs.fluid.control.WhichPort;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.util.Lattice;

public abstract class SimplifiedJavaFlowAnalysisQuery<SELF extends JavaFlowAnalysisQuery<R>, R, T, L extends Lattice<T>>
    extends AbstractJavaFlowAnalysisQuery<SELF, R, T, L> {
  /* Enumeration that provides three techniques for getting the raw result from
   * the analysis: before the entry port, after the normal exit port, or 
   * after the exceptional (abrupt) exit port.
   */
  protected static enum RawResultFactory {
    ENTRY {
      @Override
      public <T, L extends Lattice<T>, A extends IJavaFlowAnalysis<T, L>> T getRawResult(final IRNode expr, final A analysis) {
        return analysis.getAfter(expr, WhichPort.ENTRY);
      }
    },
    
    NORMAL_EXIT {
      @Override
      public <T, L extends Lattice<T>, A extends IJavaFlowAnalysis<T, L>> T getRawResult(final IRNode expr, final A analysis) {
        return analysis.getAfter(expr, WhichPort.NORMAL_EXIT);
      }
    },
    
    ABRUPT_EXIT {
      @Override
      public <T, L extends Lattice<T>, A extends IJavaFlowAnalysis<T, L>> T getRawResult(final IRNode expr, final A analysis) {
        return analysis.getAfter(expr, WhichPort.ABRUPT_EXIT);
      }
    };
    
    public abstract <T, L extends Lattice<T>, A extends IJavaFlowAnalysis<T, L>> T getRawResult(IRNode expr, A analysis);
  }

  
  
  /**
   * How to get the value from the analysis object.
   */
  private final RawResultFactory rawResultFactory;
  
  
  
  /**
   * Create a new query object based on a thunked analysis.  This is the
   * constructor that should be used when the query object is created
   * at the request of "get new query" method in the analysis.
   */
  protected SimplifiedJavaFlowAnalysisQuery(
      final IThunk<? extends IJavaFlowAnalysis<T, L>> thunk) {
    super(thunk);
    rawResultFactory = getRawResultFactory();
  }
  
  /**
   * Create a new query object based on a subanalysis query.  This should
   * only be used internally.
   */
  protected SimplifiedJavaFlowAnalysisQuery(final Delegate<SELF, R, T, L> d) {
    super(d);
    rawResultFactory = getRawResultFactory();
  }

  
  
  @Override
  protected R getBottomReturningResult(final L lattice, final IRNode expr) {
    return processRawResult(expr, lattice, lattice.bottom());
  }

  @Override
  protected R getEvaluatedAnalysisResult(
      final IJavaFlowAnalysis<T, L> analysis, final L lattice, final IRNode expr) {
    return processRawResult(expr, lattice, rawResultFactory.getRawResult(expr, analysis));
  }


  
  /**
   * Return the raw result factory used by the query. This method is called by
   * the constructor. Because this is a precarious thing to do, an
   * implementation of this method must immediately return one of
   * {@link RawResultFactory#ENTRY}, {@link RawResultFactory#NORMAL_EXIT}, or
   * {@link RawResultFactory#ABRUPT_EXIT}.
   */
  protected abstract RawResultFactory getRawResultFactory();
  
  /**
   * Massage the raw analysis result into a more useful value by running it 
   * through the lattice, etc.
   */
  protected abstract R processRawResult(IRNode expr, L lattice, T rawResult);
}
