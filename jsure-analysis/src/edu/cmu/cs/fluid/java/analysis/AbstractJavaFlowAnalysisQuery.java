package edu.cmu.cs.fluid.java.analysis;

import com.surelogic.util.IThunk;
import com.surelogic.util.Thunk;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.util.Lattice;

/**
 * Absurdly complicated to make sure that IJavaFlowAnalysis objects are obtained
 * lazily.  We need to do this otherwise analyses will be kicked of as soon as
 * a query is created.  This is not desirable because sometimes queries aren't 
 * used.  They are sometimes created ahead of time just in case they might
 * be needed because it isn't always possible to create the query when it is
 * needed due to loss of contextual information.  So a query doesn't force its
 * underlying analysis object to exist until someone calls {@link #getResultFor(IRNode)}
 * on the query.
 * 
 * <p>Four cases:
 * <ol>
 *   <li>Unevaluated analysis object
 *   <li>Unevaluated subanalysis derived from an unevaluated analysis 
 *   <li>Evaluated analysis object
 *   <li>Bottom-returning
 * </ol>
 * 
 * <p>Can be initialized in any one of three states.
 */
public abstract class AbstractJavaFlowAnalysisQuery<SELF extends JavaFlowAnalysisQuery<R>, R, T, L extends Lattice<T>>
    implements JavaFlowAnalysisQuery<R> {
  /* The core functionality is handed off to a delegate object.  This is
   * needed to handle the laziness, so that a query can start off unevaluated,
   * but change its behavior once it has been evaluated by changing the wrapped
   * delegate.
   */
  
  /*
   * A recurring issue when getting a subanalysis object is whether we need to
   * create a specialized "bottom-returning" query. This happens when the
   * getSubanalysis() method from the SubAnalysisFactory returns null. In this
   * case, we assume the subanalysis was not created because the code that
   * contains it is dead: it is in an "if (false) { ... }" statement, for
   * example. So the user can query about this code, but the control flow
   * analysis never visits it. We want to return a query that always returns
   * BOTTOM.
   * 
   * Every time we need to generate a subanalysis object we have to take the
   * above into account. Unfortunately this happens in three different places
   * that return three different type of objects: queries, delegates, and
   * analyses, respectively. It is very important all three locations get
   * updated appropriately when this class is changed.
   * 
   * A problem with the bottom-returning approach is that I cannot determine if
   * the caller of this method is just plain confused and shouldn't be asking me
   * about the subanalysis for the given "caller". Could check for this by
   * checking that caller is a descendant of the flow unit node associated with
   * the current analysis and that no instance initializer blocks come between
   * them, but that is expensive.
   */

  
  
  /* Interface for delegates.  Type parameters should be instantiated
   * to match those of the delegating query.  This class is static so that
   * instances of it can be created BEFORE instances of the query that will
   * delegate to it.  This means that the delegate methods need to be 
   * parameterized by delegating object.  We have opted to make the internals
   * of AbstractJavaFlowAnalysisQuery more complicated so that the subclasses
   * can be simpler.
   */
  protected static interface Delegate<SELF extends JavaFlowAnalysisQuery<R>, R, T, L extends Lattice<T>> {
    public R getResultFor(AbstractJavaFlowAnalysisQuery<SELF, R, T, L> owner, IRNode expr);
    public SELF getSubAnalysisQuery(AbstractJavaFlowAnalysisQuery<SELF, R, T, L> owner, IRNode caller);
  }
  
  
  
  /* Delegate for queries that always return bottom.  */
  private final static class IsBottomReturning<SELF extends JavaFlowAnalysisQuery<R>, R, T, L extends Lattice<T>> implements Delegate<SELF, R, T, L> {
    private final L lattice;
    
    public IsBottomReturning(final L l) {
      lattice = l;      
    }
    
    /**
     * Always forwards the bottom value to {@link #processRawResult}.
     */
    @Override
    public R getResultFor(final AbstractJavaFlowAnalysisQuery<SELF, R, T, L> owner, final IRNode expr) {
      return owner.getBottomReturningResult(lattice, expr);
    }

    /**
     * Bottom-returning queries never have subqueries.  It is an error if this
     * method is ever called.
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    public SELF getSubAnalysisQuery(final AbstractJavaFlowAnalysisQuery<SELF, R, T, L> owner, final IRNode caller) {
      throw new UnsupportedOperationException(
          "Cannot get a subanalysis query from a bottom-returning query");
    }
  }

  
  
  /* Delegate for queries that have an actual evaluated analysis object. */
  private final static class HasEvaluatedAnalysis<SELF extends JavaFlowAnalysisQuery<R>, R, T, L extends Lattice<T>> implements Delegate<SELF, R, T, L> {
    private final IJavaFlowAnalysis<T, L> analysis;    
    private final L lattice;
    
    public HasEvaluatedAnalysis(final IJavaFlowAnalysis<T, L> a) {
      analysis = a;
      lattice = a.getLattice();
    }
    
    /**
     * Get the result from the analysis according to 
     * {@link rawResultFactory} and forward the value to
     * {@link processRawResult}.
     */
    @Override
    public R getResultFor(final AbstractJavaFlowAnalysisQuery<SELF, R, T, L> owner, final IRNode expr) {
      return owner.getEvaluatedAnalysisResult(analysis, lattice, expr);
    }

    /**
     * If the subanalysis exists, it is already evaluated because it would been
     * created and evaluated when the analysis for this query was evaluated,
     * so we either create a query based on {@link HasEvaluatedAnalysis} or
     * {@link IsBottomReturning}. 
     */
    @Override
    public SELF getSubAnalysisQuery(final AbstractJavaFlowAnalysisQuery<SELF, R, T, L> owner, final IRNode caller) {
      final IJavaFlowAnalysis<T, L> sub =
        analysis.getSubAnalysisFactory().getSubAnalysis(caller);
      return owner.newSubAnalysisQuery(
          (sub == null)
            ? new IsBottomReturning<SELF, R, T, L>(lattice)
            : new HasEvaluatedAnalysis<SELF, R, T, L>(sub));
    }
  }
  
  
  
  /* Delegate for queries whose analysis object has not been forced to exist
   * yet.  The acquisition of the analysis object is stored in a "thunk" until
   * needed.
   */
  private final static class HasThunkedAnalysis<SELF extends JavaFlowAnalysisQuery<R>, R, T, L extends Lattice<T>> implements Delegate<SELF, R, T, L> {
    private final IThunk<? extends IJavaFlowAnalysis<T, L>> analysisThunk;
    
    public HasThunkedAnalysis(
        final IThunk<? extends IJavaFlowAnalysis<T, L>> t) {
      analysisThunk = t;
    }
    
    /**
     * Get the analysis object from the thunk and update the delegate object
     * in the referencing query to use an {@link HasEvaluatedAnalysis} delegate
     * that references the newly obtained analysis object.
     * @return The new {@link HasEvaluatedAnalysis} delegate object.
     */
    private Delegate<SELF, R, T, L> evaluateAnalysis(
        final AbstractJavaFlowAnalysisQuery<SELF, R, T, L> owner) {
      final IJavaFlowAnalysis<T, L> analysis = analysisThunk.getValue();
      final Delegate<SELF, R, T, L> newDelegate = 
        new HasEvaluatedAnalysis<SELF, R, T, L>(analysis);
      owner.resetDelegate(newDelegate);
      return newDelegate;
    }
    
    /**
     * Evaluates the thunked analysis, and then forwards the request to the
     * new delegate object.
     */
    @Override
    public R getResultFor(final AbstractJavaFlowAnalysisQuery<SELF, R, T, L> owner, final IRNode expr) {
      return evaluateAnalysis(owner).getResultFor(owner, expr);
    }

    /**
     * Return a new query whose delegate is a {@link HasThunkedSubAnalysis}.
     */
    @Override
    public SELF getSubAnalysisQuery(final AbstractJavaFlowAnalysisQuery<SELF, R, T, L> owner, final IRNode caller) {
      return owner.newSubAnalysisQuery(new HasThunkedSubAnalysis<SELF, R, T, L>(analysisThunk, caller));
    }
  }
  
  
  
  /* Delegate for sub analysis queries where the original analysis object
   * still hasn't been evaluated.  The lookup of the subanalysis query is
   * therefore delayed until {@link #getResultFor} is called.
   */
  private final static class HasThunkedSubAnalysis<SELF extends JavaFlowAnalysisQuery<R>, R, T, L extends Lattice<T>> implements Delegate<SELF, R, T, L> {
    private final IThunk<? extends IJavaFlowAnalysis<T, L>> originalAnalysisThunk;
    private final IRNode caller;
    
    public HasThunkedSubAnalysis(
        final IThunk<? extends IJavaFlowAnalysis<T, L>> t, IRNode c) {
      originalAnalysisThunk = t;
      caller = c;
    }
    
    /**
     * Evaluate the original analysis object and then try to lookup the 
     * subanalysis object.  If the object exists, we update the delegate object
     * to a new {@link HasEvaluatedAnalysis} object; if it doesn't exist, we 
     * update the delegate to a new {@link IsBottomReturning} object.
     * @return The new delegate object.
     */
    private Delegate<SELF, R, T, L> evaluateAnalysis(
        final AbstractJavaFlowAnalysisQuery<SELF, R, T, L> owner) {
      final IJavaFlowAnalysis<T, L> analysis = originalAnalysisThunk.getValue();
      final IJavaFlowAnalysis<T, L> sub =
        analysis.getSubAnalysisFactory().getSubAnalysis(caller);
      
      // Now see if we need to create a real delegate or a bottom-returning one
      final Delegate<SELF, R, T, L> newDelegate;
      if (sub == null) {
        newDelegate = new IsBottomReturning<SELF, R, T, L>(analysis.getLattice());
      } else {
        newDelegate = new HasEvaluatedAnalysis<SELF, R, T, L>(sub);
      }
      owner.resetDelegate(newDelegate);
      return newDelegate;
    }
    
    /**
     * Evaluate the analysis object, and forward the call to the new delegate
     * object.
     */
    @Override
    public R getResultFor(final AbstractJavaFlowAnalysisQuery<SELF, R, T, L> owner, final IRNode expr) {
      return evaluateAnalysis(owner).getResultFor(owner, expr);
    }

    /**
     * Return a new query whose delegate is a {@link HasThunkedSubAnalysis}. In
     * this case, we provide a new thunk for the "base analysis" object. This
     * thunk evaluates the origina thunk, and then either returns the
     * subanalysis object (if it exists) or throws an exception if the
     * subanalysys doesn't exist, because in that case we would be trying to get
     * a subanalysis from a bottom-returning query, which is an error.
     */
    @Override
    public SELF getSubAnalysisQuery(final AbstractJavaFlowAnalysisQuery<SELF, R, T, L> owner, final IRNode subCaller) {
      final IThunk<IJavaFlowAnalysis<T, L>> subThunk = 
        new Thunk<IJavaFlowAnalysis<T, L>>() {
          @Override
          protected IJavaFlowAnalysis<T, L> evaluate() {
            final IJavaFlowAnalysis<T, L> originalAnalysis =
              originalAnalysisThunk.getValue();
            final IJavaFlowAnalysis<T, L> subAnalysis1 =
              originalAnalysis.getSubAnalysisFactory().getSubAnalysis(caller);
            if (subAnalysis1 == null) {
              throw new UnsupportedOperationException(
                  "Cannot get a subanalysis query from a bottom-returning query");
            } else {
              return subAnalysis1;
            }
          }
        };
      return owner.newSubAnalysisQuery(new HasThunkedSubAnalysis<SELF, R, T, L>(subThunk, subCaller));
    }
  }


    
  /**
   * All the magic happens in the delegate.  This lets us change our
   * behavior dynamically.
   */
  private Delegate<SELF, R, T, L> delegate;
  
  
  
  /**
   * Create a new query object based on a thunked analysis.  This is the
   * constructor that should be used when the query object is created
   * at the request of "get new query" method in the analysis.
   */
  protected AbstractJavaFlowAnalysisQuery(
      final IThunk<? extends IJavaFlowAnalysis<T, L>> thunk) {
    this(new HasThunkedAnalysis<SELF, R, T, L>(thunk));
  }
  
  /**
   * Create a new query object based on a subanalysis query.  This should
   * only be used internally.
   */
  protected AbstractJavaFlowAnalysisQuery(final Delegate<SELF, R, T, L> d) {
    delegate = d;
  }
  
  
  
  private void resetDelegate(final Delegate<SELF, R, T, L> newDelegate) {
    delegate = newDelegate;
  }
  
  
  
  @Override
  public final R getResultFor(final IRNode expr) {
    return delegate.getResultFor(this, expr);
  }

  @Override
  public final SELF getSubAnalysisQuery(final IRNode caller) {
    return delegate.getSubAnalysisQuery(this, caller);
  }

  
  
  protected abstract R getBottomReturningResult(L lattice, IRNode expr);

  protected abstract R getEvaluatedAnalysisResult(
      IJavaFlowAnalysis<T, L> analysis, L lattice, IRNode expr);



  protected abstract SELF newSubAnalysisQuery(Delegate<SELF, R, T, L> delegate);
}
