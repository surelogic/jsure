package com.surelogic.analysis.concurrency.heldlocks_new;

import java.util.Collections;
import java.util.Set;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.concurrency.heldlocks_new.LockUtils.LockMethods;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLock;
import com.surelogic.util.IThunk;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.analysis.JavaFlowAnalysisQuery;
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
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardTransfer;

public final class MustHoldAnalysis extends
    edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice, JavaForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice>> {
  private final static HeldLocks EMPTY_HELD_LOCKS = new HeldLocks(
      Collections.<HeldLock>emptySet(),
      Collections.<HeldLock>emptySet(),
      Collections.<HeldLock>emptySet());
  
  public final static JavaFlowAnalysisQuery<HeldLocks> EMPTY_HELD_LOCKS_QUERY = new EmptyHeldLocksQuery();
  
  
  
  public final static class HeldLocks {
    public final Set<HeldLock> heldLocks;
    public final Set<HeldLock> classInitLocks;
    public final Set<HeldLock> singleThreadedLocks;
    
    public HeldLocks(final Set<HeldLock> held, final Set<HeldLock> classInit,
        final Set<HeldLock> singleThreaded) {
      heldLocks = held;
      classInitLocks = classInit;
      singleThreadedLocks = singleThreaded;
    }
  }
  
  
  private final static class EmptyHeldLocksQuery implements JavaFlowAnalysisQuery<HeldLocks> {
    @Override
    public EmptyHeldLocksQuery getSubAnalysisQuery(final IRNode caller) {
      return this;
    }

    @Override
    public HeldLocks getResultFor(final IRNode expr) {
      return EMPTY_HELD_LOCKS;
    }    
  }
  
  
  public final class LocksForQuery extends SimplifiedJavaFlowAnalysisQuery<LocksForQuery, Set<IRNode>, ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> {
    public LocksForQuery(
        final IThunk<? extends IJavaFlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice>> t) {
      super(t);
    }

    private LocksForQuery(final Delegate<LocksForQuery, Set<IRNode>, ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> d) {
      super(d);
    }

    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.ENTRY;
    }


    
    @Override
    protected LocksForQuery newSubAnalysisQuery(final Delegate<LocksForQuery, Set<IRNode>, ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> d) {
      return new LocksForQuery(d);
    }
    

    
    @Override
    protected Set<IRNode> processRawResult(final IRNode mcall,
        final MustHoldLattice lattice,
        final ImmutableList<ImmutableSet<IRNode>>[] rawResult) {
      final MethodCall call = (MethodCall) tree.getOperator(mcall);
      return lattice.getLocksFor(rawResult, call.get_Object(mcall));
    }
  }
  
  

  public final class HeldLocksQuery extends SimplifiedJavaFlowAnalysisQuery<HeldLocksQuery, HeldLocks, ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> {
    public HeldLocksQuery(
        final IThunk<? extends IJavaFlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice>> t) {
      super(t);
    }
    
    private HeldLocksQuery(final Delegate<HeldLocksQuery, HeldLocks, ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> d) {
      super(d);
    }

    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.ENTRY;
    }
    
    
    
    @Override
    protected HeldLocksQuery newSubAnalysisQuery(final Delegate<HeldLocksQuery, HeldLocks, ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> d) {
      return new HeldLocksQuery(d);
    }


    
    @Override
    protected HeldLocks processRawResult(final IRNode mcall,
        final MustHoldLattice lattice,
        final ImmutableList<ImmutableSet<IRNode>>[] rawResult) {
      return new HeldLocks(
          lattice.getHeldLocks(rawResult),
          lattice.getClassInit(),
          lattice.getSingleThreaded());
    }
  }


  
  private final ThisExpressionBinder thisExprBinder;
  private final LockUtils lockUtils;
  private final LockExpressionManager lockExprManager;
  private final SimpleNonnullAnalysis nonNullAnalysis;

  
  
  public MustHoldAnalysis(final ThisExpressionBinder teb, final LockUtils lu,
      final LockExpressionManager lockMgr, final SimpleNonnullAnalysis sna) {
    super(teb);
    thisExprBinder = teb;
    lockUtils = lu;
    lockExprManager = lockMgr;
    nonNullAnalysis = sna;
    
  }

  @Override
  protected JavaForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> createAnalysis(
      final IRNode flowUnit) {
    final MustHoldLattice mustHoldLattice =
      MustHoldLattice.createForFlowUnit(flowUnit, thisExprBinder, lockExprManager
          );    
    final JavaForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> analysis =
      new JavaForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice>(
        "Must Hold Analysis", mustHoldLattice,
        new MustHoldTransfer(thisExprBinder, lockUtils, mustHoldLattice,
            nonNullAnalysis.getNonnullBeforeQuery(flowUnit)), DebugUnparser.viewer);
    return analysis;
  }

  public LocksForQuery getLocksForQuery(final IRNode flowUnit) {
    return new LocksForQuery(getAnalysisThunk(flowUnit));
  }
  
  public HeldLocksQuery getHeldLocksQuery(final IRNode flowUnit) {
    return new HeldLocksQuery(getAnalysisThunk(flowUnit));
  }

  
  
  private static final class MustHoldTransfer extends
      JavaForwardTransfer<MustHoldLattice, ImmutableList<ImmutableSet<IRNode>>[]> {
    private final LockUtils lockUtils;
    private final SimpleNonnullAnalysis.Query nonNullAnalysisQuery;

    
    
    public MustHoldTransfer(
        final IBinder binder, final LockUtils lu,
        final MustHoldLattice lattice, final SimpleNonnullAnalysis.Query query) {
      super(binder, lattice, new SubAnalysisFactory(lu, query));
      lockUtils = lu;
      nonNullAnalysisQuery = query;
    }
    
    
    
    @Override
    protected ImmutableList<ImmutableSet<IRNode>>[] transferAssignment(
        IRNode node, ImmutableList<ImmutableSet<IRNode>>[] value) {
      // by default return the same value:
      return value;
    }

    @Override
    protected ImmutableList<ImmutableSet<IRNode>>[] transferInitialization(
        IRNode node, ImmutableList<ImmutableSet<IRNode>>[] value) {
      // by default return the same value:
      return value;
    }
    
    @Override
    public ImmutableList<ImmutableSet<IRNode>>[] transferConditional(
        final IRNode node, final boolean flag, 
        final ImmutableList<ImmutableSet<IRNode>>[] before) {
      /* We only do interesting things if node is a method call node for
       * a tryLock() call.
       */
      final Operator op = tree.getOperator(node);
      if (op instanceof MethodCall) {
        final LockMethods lockMethod = lockUtils.whichLockMethod(node);
        if (lockMethod == LockMethods.TRYLOCK) {
          /* Here the problem is that transferCall(), which is called
           * immediately before transferConditional() in this case, has already
           * pushed a lock acquisition on the stack, even though the acquisition
           * only holds along the true-branch of the conditional.  So we need 
           * to remove the acquisition along the false-branch by pretending
           * the else-branch starts with an immediate lock release.
           */
          if (!flag) { // else branch
            /* XXX: This is possibly sleazy: foundUnlock doesn't currently care
             * whether the given method is a lock or unlock, but if it does in
             * the future, we could have problems because we are giving it the
             * node for a tryLock() call.
             */
            final ImmutableList<ImmutableSet<IRNode>>[] out = lattice.foundUnlock(before, node);
            return out;
          }
        }
      }
      
      // Normal case, always return what came into the conditional
      return before;
    }
    
    @Override
    protected ImmutableList<ImmutableSet<IRNode>>[] transferIsObject(
        IRNode node, boolean flag, ImmutableList<ImmutableSet<IRNode>>[] value) {
      if (!flag) {
        /* Abrupt case. Return BOTTOM if we can determine that the object must
         * not be null. We determine the object is non-null (1) if the object is
         * referenced by a final field that is initialized to (a) a new object
         * or (b) to the result of calling ReadWriteLock.readLock() or
         * ReadWriteLock.writeLock(); (2) if the object is a method call to a
         * method with a returns lock annotation; or (3) if the object is
         * referenced by a local variable that is known to be non-null.
         */
        final Operator operator = JJNode.tree.getOperator(node);
        if (FieldRef.prototype.includes(operator)) {
          final IRNode varDecl = binder.getBinding(node);
          if (TypeUtil.isJSureFinal(varDecl)) { // Final field, check initialization
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
                  lockUtils.isMethodFromJavaUtilConcurrentLocksReadWriteLock(initValue)) {
                return lattice.bottom();
              }
            }
          }
        } else if (MethodCall.prototype.includes(operator)) {
          if (lockUtils.isLockGetterMethod(node) || lockUtils.isMethodFromJavaUtilConcurrentLocksReadWriteLock(node)) {
            // Lock getter methods do not return null values.
            return lattice.bottom();
          }
        } else if (VariableUseExpression.prototype.includes(operator)) {
          final Set<IRNode> nonNull = nonNullAnalysisQuery.getResultFor(node);
          final IRNode varDecl = binder.getBinding(node);
          if (nonNull.contains(varDecl)) {
            return lattice.bottom();
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
      final Operator op = tree.getOperator(call);
      if (op instanceof MethodCall) {
        // Method call: check to see if it is a lock or an unlock
        final LockMethods lockMethod = lockUtils.whichLockMethod(call);
        if (lockMethod != LockMethods.NOT_A_LOCK_METHOD && lockMethod != LockMethods.IDENTICALLY_NAMED_METHOD) {
          if (lockMethod != LockMethods.UNLOCK) { // Already weeded out NOT_A_LOCK_METHOD and IDENTICALLY_NAMED
            // For exceptional termination, the lock is not acquired
            if (flag) {
              final ImmutableList<ImmutableSet<IRNode>>[] newValue =
                lattice.foundLock(value, call);
              return newValue;
            } else {
              return value;
            }
          } else { // Must be unlock()
            // The lock is always released, even for abrupt termination.
            final ImmutableList<ImmutableSet<IRNode>>[] newValue =
              lattice.foundUnlock(value, call);
            return newValue;
          }          
        } else {
          // Not a lock or an unlock; is it a lock getter call?
          if (lockUtils.isLockGetterMethod(call) || lockUtils.isMethodFromJavaUtilConcurrentLocksReadWriteLock(call)) {
            /* In the abrupt case, return bottom to indicate that lock getter
             * methods don't throw exceptions.
             */
            return (flag ? value : lattice.bottom());
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

    @Override
    public ImmutableList<ImmutableSet<IRNode>>[] transferComponentSource(IRNode node) {
      // Initial state of affairs is no locks held
      ImmutableList<ImmutableSet<IRNode>>[] initValue = lattice.getEmptyValue();
      final IRNode flowUnit = lattice.getFlowUnit();
      
      for (HeldLock requiredLock : lattice.getRequiredLocks()) {
        final int idx = lattice.indexOf(requiredLock);
        if (idx != -1) {
          // Push the lock precondition onto the stack as a new singleton set
          initValue = lattice.replaceValue(initValue, idx,
              lattice.getBaseLattice().push(initValue[idx],
                  ImmutableHashOrderSet.<IRNode>emptySet().addCopy(flowUnit)));
        }
      }
      return initValue;
    }
  }



  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<MustHoldLattice, ImmutableList<ImmutableSet<IRNode>>[]> {
    private final LockUtils lockUtils;
    private final SimpleNonnullAnalysis.Query query;
    
    public SubAnalysisFactory(
        final LockUtils lu, final SimpleNonnullAnalysis.Query q) {
      lockUtils = lu;
      query = q;
    }
    
    @Override
    protected JavaForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> realCreateAnalysis(
        final IRNode caller, final IBinder binder,
        final MustHoldLattice lattice,
        final ImmutableList<ImmutableSet<IRNode>>[] initialValue,
        final boolean terminationNormal) {
      return new JavaForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice>(
          "Must Hold Analysis (sub-analysis)", lattice,
          new MustHoldTransfer(binder, lockUtils, lattice,
              query.getSubAnalysisQuery(caller)), DebugUnparser.viewer);
    }
    
  }
}

