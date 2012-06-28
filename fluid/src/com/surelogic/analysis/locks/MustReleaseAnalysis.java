package com.surelogic.analysis.locks;

import java.util.Set;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.util.IThunk;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.analysis.SimplifiedJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.ReturnsLockPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaBackwardAnalysis;
import edu.uwm.cs.fluid.java.control.JavaBackwardTransfer;

public final class MustReleaseAnalysis extends
    edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustReleaseLattice, JavaBackwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustReleaseLattice>> {
  public final class Query extends SimplifiedJavaFlowAnalysisQuery<Query, Set<IRNode>, ImmutableList<ImmutableSet<IRNode>>[], MustReleaseLattice> {
    public Query(
        final IThunk<? extends IJavaFlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustReleaseLattice>> thunk) {
      super(thunk);
    }

    private Query(final Delegate<Query, Set<IRNode>, ImmutableList<ImmutableSet<IRNode>>[], MustReleaseLattice> d) {
      super(d);
    }

    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }
    


    @Override
    protected Query newSubAnalysisQuery(final Delegate<Query, Set<IRNode>, ImmutableList<ImmutableSet<IRNode>>[], MustReleaseLattice> d) {
      return new Query(d);
    }
    


    @Override
    protected Set<IRNode> processRawResult(final IRNode mcall,
        final MustReleaseLattice lattice,
        final ImmutableList<ImmutableSet<IRNode>>[] rawResult) {
      final MethodCall call = (MethodCall) tree.getOperator(mcall);
      final Set<IRNode> unlockCalls =
        lattice.getUnlocksFor(rawResult, call.get_Object(mcall), thisExprBinder, binder);
      /* Remove ourself from the set---this will happen in the case of a 
       * tryLock() embedded in an if-statement, see 
       * MustReleaseTransfer.transferConditional().
       */
      if (unlockCalls != null) unlockCalls.remove(mcall);
      return unlockCalls;
    }    
  }

  
  
  private final ThisExpressionBinder thisExprBinder; 
  private final LockUtils lockUtils;
  private final JUCLockUsageManager jucLockUsageManager;
  private final SimpleNonnullAnalysis nonNullAnalysis;
  
  
  
  public MustReleaseAnalysis(final ThisExpressionBinder teb, final IBinder b, final LockUtils lu,
      final JUCLockUsageManager lockMgr, final SimpleNonnullAnalysis sna) {
    super(b);
    thisExprBinder = teb;
    lockUtils = lu;
    jucLockUsageManager = lockMgr;
    nonNullAnalysis = sna;
  }

  
  @Override
  protected JavaBackwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustReleaseLattice> createAnalysis(final IRNode flowUnit) {
    final MustReleaseLattice mustReleaseLattice =
      MustReleaseLattice.createForFlowUnit(flowUnit, thisExprBinder, binder, jucLockUsageManager);    
    final JavaBackwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustReleaseLattice> analysis =
      new JavaBackwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustReleaseLattice>(
        "Must Release Analysis", mustReleaseLattice,
        new MustReleaseTransfer(
            thisExprBinder, binder, lockUtils, mustReleaseLattice,
            nonNullAnalysis.getNonnullBeforeQuery(flowUnit)), DebugUnparser.viewer);
    return analysis;
  }

//  /**
//   * Get the matching unlock.
//   * @return The IRNode of the matching unlock method call.
//   */
//  public Set<IRNode> getUnlocksFor(final IRNode mcall, final IRNode context) {
//    final JavaBackwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustReleaseLattice> a =
//      getAnalysis(
//        edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis.getFlowUnit(
//            mcall, context));
//    final MustReleaseLattice lattice = a.getLattice();   
//    final MethodCall call = (MethodCall) tree.getOperator(mcall);
//    final ImmutableList<ImmutableSet<IRNode>>[] value = a.getAfter(mcall, WhichPort.NORMAL_EXIT);
//    final Set<IRNode> unlockCalls =
//      lattice.getUnlocksFor(value, call.get_Object(mcall), thisExprBinder, binder);
//    /* Remove ourself from the set---this will happen in the case of a 
//     * tryLock() embedded in an if-statement, see 
//     * MustReleaseTransfer.transferConditional().
//     */
//    if (unlockCalls != null) unlockCalls.remove(mcall);
//    return unlockCalls;
//  }
  
  /**
   * @param flowUnit
   *          The MethodDeclaration, ConstructorDeclaration,
   *          ClassInitDeclaration, or InitDeclaration to be analyzed.
   * @param initializer
   *          Must be <code>false</code> if <code>flowUnit</code> is not a
   *          ConstructorDeclaration. Otherwise, this indicates whether we are
   *          actually interested in looking at the contents of the field
   *          initializers and instance initializers visited on behalf of the
   *          constructor.
   */
  public Query getUnlocksForQuery(final IRNode flowUnit) {
    return new Query(getAnalysisThunk(flowUnit));
  }
  
  
  
  private static final class MustReleaseTransfer extends
      JavaBackwardTransfer<MustReleaseLattice, ImmutableList<ImmutableSet<IRNode>>[]> {
    private final ThisExpressionBinder thisExprBinder;
    private final LockUtils lockUtils;
    private final SimpleNonnullAnalysis.Query nonNullAnalysisQuery;

    
    
    public MustReleaseTransfer(
        final ThisExpressionBinder teb, final IBinder binder, final LockUtils lu,
        final MustReleaseLattice lattice,
        final SimpleNonnullAnalysis.Query query) {
      super(binder, lattice, new SubAnalysisFactory(teb, lu, query));
      thisExprBinder = teb;
      lockUtils = lu;
      nonNullAnalysisQuery = query;
    }
    
    
    
    /**
     * Is the method declared to return a lock?
     */
    private boolean isLockGetterMethod(final IRNode mcall) {
      final IRNode mdecl = binder.getBinding(mcall);
      final ReturnsLockPromiseDrop returnedLock = LockUtils.getReturnedLock(mdecl);
      return (returnedLock != null);
    }
    
    public ImmutableList<ImmutableSet<IRNode>>[] transferConditional(
        final IRNode node, final boolean flag, 
        final ImmutableList<ImmutableSet<IRNode>>[] after) {
      // Bail out if input is top or bottom
      if (!lattice.isNormal(after)) {
        return after;
      }

      /* We only do interesting things if node is a method call node for
       * a tryLock() call.
       */
      final Operator op = tree.getOperator(node);
      if (op instanceof MethodCall) {
        final LockMethods lockMethod = lockUtils.whichLockMethod(node);
        if (lockMethod == LockMethods.TRY_LOCK) {
          /* Here the problem is that transferCall() will be called after
           * transferConditional() and always acquire the lock.  So we must
           * push an unlock() on the stack so that the tryLock() will have an
           * unlock() to match up with.  (In a well-formed true-branch, the
           * programmer will have already made sure there is an unlock() call.  
           */
          if (!flag) { // else branch
            /* XXX: This is possibly sleazy: foundUnlock doesn't currently care
             * whether the given method is a lock or unlock, but if it does in
             * the future, we could have problems because we are giving it the
             * node for a tryLock() call.
             */
            final ImmutableList<ImmutableSet<IRNode>>[] newValue =
              lattice.foundUnlock(after, node, thisExprBinder, binder);
            return newValue;
          }
        }
      }
      
      // Normal case, always return what came into the conditional
      return after;
    }
    
    @Override
    protected ImmutableList<ImmutableSet<IRNode>>[] transferIsObject(
        final IRNode node, final boolean flag,
        final ImmutableList<ImmutableSet<IRNode>>[] value) {
      // Bail out if input is top or bottom
      if (!lattice.isNormal(value)) {
        return value;
      }
      
      if (!flag) {
        /* Abrupt case. Return BOTTOM if we can determine that the object must
         * not be null. We determine the object is non-null (1) if the object is
         * referenced by a final field that is initialized to (a) a new object
         * or (b) to the result of calling ReadWriteLock.readLock() or
         * ReadWriteLock.writeLock(); (2) if the object is a method call to a
         * method with a returns lock annotation; or (3) if the object is
         * referenced by a local variable that is known to be non-null.
         */
        final ImmutableList<ImmutableSet<IRNode>>[] bottom = lattice.bottom();
        final Operator operator = JJNode.tree.getOperator(node);
        if (FieldRef.prototype.includes(operator)) {
          final IRNode varDecl = binder.getBinding(node);
          if (TypeUtil.isFinal(varDecl)) { // Final field, check initialization
            final IRNode init = VariableDeclarator.getInit(varDecl);
            if (Initialization.prototype.includes(init)) {
              final IRNode initValue = Initialization.getValue(init);
              final Operator initValueOp = JJNode.tree.getOperator(initValue);
              // (1a) Initialized to new object
              if (NewExpression.prototype.includes(initValueOp) ||
                  AnonClassExpression.prototype.includes(initValueOp)) {
                return lattice.bottom();
              }
              
              /* (1b) Initialized to ReadWriteLock.readLock() or
               * ReadWriteLock.writeLock()
               */
              if (MethodCall.prototype.includes(initValueOp) &&
                  lockUtils.isJUCRWMethod(initValue)) {
                return lattice.bottom();
              }

            }
          }
        } else if (MethodCall.prototype.includes(operator)) {
          if (isLockGetterMethod(node) || lockUtils.isJUCRWMethod(node)) {
            // Lock getter methods do not return null values.
            return bottom;
          }
        } else if (VariableUseExpression.prototype.includes(operator)) {
          final Set<IRNode> nonNull = nonNullAnalysisQuery.getResultFor(node);
          final IRNode varDecl = binder.getBinding(node);
          if (nonNull.contains(varDecl)) {
            return bottom;
          }
        }
      }
      return value;
    }

    
    @Override
    protected ImmutableList<ImmutableSet<IRNode>>[] transferCall(
        final IRNode call, final boolean flag,
        final ImmutableList<ImmutableSet<IRNode>>[] value) {
      /* N.B. Don't have to also override transferImpliedNewExpression because
       * we don't care about new expressions.
       */
      
      // Bail out if input is top or bottom
      if (!lattice.isNormal(value)) {
        return value;
      }
      
      final Operator op = tree.getOperator(call);
      if (op instanceof MethodCall) {
        // Method call: check to see if it is a lock or an unlock
        final LockMethods lockMethod = lockUtils.whichLockMethod(call);
        if (lockMethod != LockMethods.NOT_A_LOCK_METHOD && lockMethod != LockMethods.IDENTICALLY_NAMED_METHOD) {
          if (lockMethod.isLock) {
            // For exceptional termination, the lock is not acquired
            if (flag) {
              final ImmutableList<ImmutableSet<IRNode>>[] newValue =
                lattice.foundLock(value, call, thisExprBinder, binder);
              return newValue;
            } else {
              return value;
            }
          } else { // Must be unlock()
            // The lock is always released, even for abrupt termination.
            final ImmutableList<ImmutableSet<IRNode>>[] newValue =
              lattice.foundUnlock(value, call, thisExprBinder, binder);
            return newValue;
          }
        } else {
          // Not a lock or an unlock; is it a lock getter call?
          if (isLockGetterMethod(call) || lockUtils.isJUCRWMethod(call)) {
            /* In the abrupt case, return bottom to indicate that lock getter
             * methods don't throw exceptions.
             */
            final ImmutableList<ImmutableSet<IRNode>>[] newValue = (flag ? value : lattice.bottom());
            return newValue;
          } else {
            // Otherwise, not interesting
            return value;
          }
        }
      } else {
        // Constructor calls are not interesting
        return value;
      }
    }

    public ImmutableList<ImmutableSet<IRNode>>[] transferComponentSink(IRNode node, boolean normal) {
      final ImmutableList<ImmutableSet<IRNode>>[] emptyValue = lattice.getEmptyValue();
      return emptyValue;
    }
  }



  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<MustReleaseLattice, ImmutableList<ImmutableSet<IRNode>>[]> {
    private final ThisExpressionBinder thisExprBinder;
    private final LockUtils lockUtils;
    private final SimpleNonnullAnalysis.Query query;
    
    public SubAnalysisFactory(
        final ThisExpressionBinder teb, final LockUtils lu,
        final SimpleNonnullAnalysis.Query q) {
      thisExprBinder = teb;
      lockUtils = lu;
      query = q;
    }
    
    @Override
    protected JavaBackwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustReleaseLattice> realCreateAnalysis(
        final IRNode caller, final IBinder binder,
        final MustReleaseLattice lattice,
        final ImmutableList<ImmutableSet<IRNode>>[] initialValue,
        final boolean terminationNormal) {
      return new JavaBackwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustReleaseLattice>("Must Release Analysis (sub-analysis)", lattice,
          new MustReleaseTransfer(thisExprBinder, binder, lockUtils, lattice,
              query.getSubAnalysisQuery(caller)), DebugUnparser.viewer);
    }
    
  }
}

