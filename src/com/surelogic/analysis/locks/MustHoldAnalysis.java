package com.surelogic.analysis.locks;

import java.util.Collections;
import java.util.Set;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.locks.locks.HeldLock;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.analysis.AbstractJavaFlowAnalysisQuery;
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
  public final static HeldLocks EMPTY_HELD_LOCKS = new HeldLocks(
      Collections.<HeldLock>emptySet(),
      Collections.<HeldLock>emptySet(),
      Collections.<HeldLock>emptySet());
  
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
  
  
  
  public final class LocksForQuery extends AbstractJavaFlowAnalysisQuery<LocksForQuery, Set<IRNode>, ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> {
    protected LocksForQuery(
        final IJavaFlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> a) {
      super(a, RawResultFactory.ENTRY);
    }

    private LocksForQuery(final MustHoldLattice lattice) {
      super(lattice);
    }

    @Override
    protected LocksForQuery newAnalysisBasedSubQuery(
        final IJavaFlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> subAnalysis) {
      return new LocksForQuery(subAnalysis);
    }

    @Override
    protected LocksForQuery newBottomReturningSubQuery(final MustHoldLattice lattice) {
      return new LocksForQuery(lattice);
    }

    @Override
    protected Set<IRNode> processRawResult(final IRNode mcall,
        final ImmutableList<ImmutableSet<IRNode>>[] rawResult) {
      final MethodCall call = (MethodCall) tree.getOperator(mcall);
      return lattice.getLocksFor(rawResult, call.get_Object(mcall), thisExprBinder, binder);
    }
  }
  
  

  public final class HeldLocksQuery extends AbstractJavaFlowAnalysisQuery<HeldLocksQuery, HeldLocks, ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> {
    protected HeldLocksQuery(
        final IJavaFlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> a) {
      super(a, RawResultFactory.ENTRY);
    }

    private HeldLocksQuery(final MustHoldLattice lattice) {
      super(lattice);
    }

    @Override
    protected HeldLocksQuery newAnalysisBasedSubQuery(
        final IJavaFlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> subAnalysis) {
      return new HeldLocksQuery(subAnalysis);
    }

    @Override
    protected HeldLocksQuery newBottomReturningSubQuery(final MustHoldLattice lattice) {
      return new HeldLocksQuery(lattice);
    }

    @Override
    protected HeldLocks processRawResult(final IRNode mcall,
        final ImmutableList<ImmutableSet<IRNode>>[] rawResult) {
      return new HeldLocks(
          lattice.getHeldLocks(rawResult),
          lattice.getClassInit(),
          lattice.getSingleThreaded());
    }
  }

//  public static final class Analysis extends JavaForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> {
//    private Analysis(
//        final String name, final MustHoldLattice l, final MustHoldTransfer t) {
//      super(name, l, t, DebugUnparser.viewer);
//    }
//  }
  
  
  
  private final ThisExpressionBinder thisExprBinder;
  private final LockUtils lockUtils;
  private final JUCLockUsageManager jucLockUsageManager;
  private final SimpleNonnullAnalysis nonNullAnalysis;

  
  
  public MustHoldAnalysis(final ThisExpressionBinder teb, final IBinder b, final LockUtils lu,
      final JUCLockUsageManager lockMgr, final SimpleNonnullAnalysis sna) {
    super(b);
    thisExprBinder = teb;
    lockUtils = lu;
    jucLockUsageManager = lockMgr;
    nonNullAnalysis = sna;
    
  }

  @Override
  protected JavaForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> createAnalysis(
      final IRNode flowUnit) {
    final MustHoldLattice mustHoldLattice =
      MustHoldLattice.createForFlowUnit(flowUnit, thisExprBinder, binder, jucLockUsageManager);    
    final JavaForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> analysis =
      new JavaForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice>(
        "Must Hold Analysis", mustHoldLattice,
        new MustHoldTransfer(thisExprBinder, binder, lockUtils, mustHoldLattice,
            nonNullAnalysis.getNonnullBeforeQuery(flowUnit)), DebugUnparser.viewer);
    return analysis;
  }

//  /**
//   * Given an unlock call, get the set of corresponding lock calls that it
//   * might release. 
//   * @return The IRNodes of the matching lock method call.
//   */
//  public Set<IRNode> getLocksFor(final IRNode mcall, final IRNode context) {
//    final JavaForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> a =
//      getAnalysis(
//          edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis.getFlowUnit(
//              mcall, context));
//    final MustHoldLattice mhl = a.getLattice();
//    final MethodCall call = (MethodCall) tree.getOperator(mcall);
//    final ImmutableList<ImmutableSet<IRNode>>[] value = a.getAfter(mcall, WhichPort.ENTRY);
//    return mhl.getLocksFor(value, call.get_Object(mcall), thisExprBinder, binder);
//  }
  
  public LocksForQuery getLocksForQuery(final IRNode flowUnit) {
    return new LocksForQuery(getAnalysis(flowUnit));
  }
  
//  /**
//   * Given a node, find the set of locks expressions that are locked on
//   * entry to the node.
//   */
//  public Set<HeldLock> getHeldLocks(final IRNode node, final IRNode context) {
//    final JavaForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> a =
//      getAnalysis(
//          edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis.getFlowUnit(
//              node, context));
//    final MustHoldLattice mhl = a.getLattice();
//    return mhl.getHeldLocks(a.getAfter(node, WhichPort.ENTRY));
//  }
  
  public HeldLocksQuery getHeldLocksQuery(final IRNode flowUnit) {
    return new HeldLocksQuery(getAnalysis(flowUnit));
  }

  
  
  private static final class MustHoldTransfer extends
      JavaForwardTransfer<MustHoldLattice, ImmutableList<ImmutableSet<IRNode>>[]> {
    private final ThisExpressionBinder thisExprBinder;
    private final LockUtils lockUtils;
    private final SimpleNonnullAnalysis.Query nonNullAnalysisQuery;

    
    
    public MustHoldTransfer(
        final ThisExpressionBinder teb, final IBinder binder, final LockUtils lu,
        final MustHoldLattice lattice, final SimpleNonnullAnalysis.Query query) {
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

    @Override
    protected ImmutableList<ImmutableSet<IRNode>>[] transferAssignment(
        IRNode node, ImmutableList<ImmutableSet<IRNode>>[] value) {
//      System.out.println("transferAssignment: " + lattice.toString(value));
//      System.out.flush();
      
      // by default return the same value:
      return value;
    }

    @Override
    protected ImmutableList<ImmutableSet<IRNode>>[] transferInitialization(
        IRNode node, ImmutableList<ImmutableSet<IRNode>>[] value) {
//      System.out.println("transferInitialization: " + lattice.toString(value));
//      System.out.flush();

      // by default return the same value:
      return value;
    }
    
    @Override
    public ImmutableList<ImmutableSet<IRNode>>[] transferConditional(
        final IRNode node, final boolean flag, 
        final ImmutableList<ImmutableSet<IRNode>>[] before) {
//      System.out.println("transferConditional: " + lattice.toString(before));
//      System.out.flush();

      /* We only do interesting things if node is a method call node for
       * a tryLock() call.
       */
      final Operator op = tree.getOperator(node);
      if (op instanceof MethodCall) {
        final LockMethods lockMethod = lockUtils.whichLockMethod(node);
        if (lockMethod == LockMethods.TRY_LOCK) {
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
            final ImmutableList<ImmutableSet<IRNode>>[] out = lattice.foundUnlock(before, node, thisExprBinder, binder);

//            System.out.println("  " + lattice.toString(out));
//            System.out.flush();

            return out;
          }
        }
      }
      
      // Normal case, always return what came into the conditional
//      System.out.println("  " + lattice.toString(before));
//      System.out.flush();
      return before;
    }
    
    @Override
    protected ImmutableList<ImmutableSet<IRNode>>[] transferIsObject(
        IRNode node, boolean flag, ImmutableList<ImmutableSet<IRNode>>[] value) {
//      System.out.println("transferIsObject (" + flag + ") " + DebugUnparser.toString(node) + ": " + lattice.toString(value));
//      System.out.flush();
      
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
          if (TypeUtil.isFinal(varDecl)) { // Final field, check initialization
            final IRNode init = VariableDeclarator.getInit(varDecl);
            if (Initialization.prototype.includes(init)) {
              final IRNode initValue = Initialization.getValue(init);
              final Operator initValueOp = JJNode.tree.getOperator(initValue);
              // (1a) Initialized to new object
              if (NewExpression.prototype.includes(initValueOp) ||
                  AnonClassExpression.prototype.includes(initValueOp)) {
//                System.out.println("  " + lattice.toString(lattice.bottom()));
//                System.out.flush();
                return lattice.bottom();
              }
              
              /* (1b) Initialized to ReadWriteLock.readLock() or
               * ReadWriteLock.writeLock()
               */
              if (MethodCall.prototype.includes(initValueOp) &&
                  lockUtils.isJUCRWMethod(initValue)) {
//                System.out.println("  " + lattice.toString(lattice.bottom()));
//                System.out.flush();
                return lattice.bottom();
              }
            }
          }
        } else if (MethodCall.prototype.includes(operator)) {
          if (isLockGetterMethod(node) || lockUtils.isJUCRWMethod(node)) {
            // Lock getter methods do not return null values.
//            System.out.println("  " + lattice.toString(lattice.bottom()));
//            System.out.flush();
            return lattice.bottom();
          }
        } else if (VariableUseExpression.prototype.includes(operator)) {
          final Set<IRNode> nonNull = nonNullAnalysisQuery.getResultFor(node);
          final IRNode varDecl = binder.getBinding(node);
          if (nonNull.contains(varDecl)) {
//            System.out.println("  " + lattice.toString(lattice.bottom()));
//            System.out.flush();
            return lattice.bottom();
          }
        }
      }
//      System.out.println("  " + lattice.toString(value));
//      System.out.flush();
      return value;
    }

    @Override
    protected ImmutableList<ImmutableSet<IRNode>>[] transferCall(
        final IRNode call, final boolean flag,
        final ImmutableList<ImmutableSet<IRNode>>[] value) {
//      System.out.println("transferCall (" + flag + ") " + DebugUnparser.toString(call) + ": " + lattice.toString(value));
//      System.out.flush();

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
//              System.out.println("  " + lattice.toString(newValue));
//              System.out.flush();
              return newValue;
            } else {
//              System.out.println("  " + lattice.toString(value));
//              System.out.flush();
              return value;
            }
          } else { // Must be unlock()
            // The lock is always released, even for abrupt termination.
            final ImmutableList<ImmutableSet<IRNode>>[] newValue =
              lattice.foundUnlock(value, call, thisExprBinder, binder);
//            System.out.println("  " + lattice.toString(newValue));
//            System.out.flush();
            return newValue;
          }          
        } else {
          // Not a lock or an unlock; is it a lock getter call?
          if (isLockGetterMethod(call) || lockUtils.isJUCRWMethod(call)) {
            /* In the abrupt case, return bottom to indicate that lock getter
             * methods don't throw exceptions.
             */
//            System.out.println("  " + lattice.toString(flag ? value : lattice.bottom()));
//            System.out.flush();
            return (flag ? value : lattice.bottom());
          } else {
            // Otherwise, not interesting
//            System.out.println("  " + lattice.toString(value));
//            System.out.flush();
            return value;
          }
        }
      } else {
        // Constructor calls are not interesting
//        System.out.println("  " + lattice.toString(value));
//        System.out.flush();
        return value;
      }
    }

    public ImmutableList<ImmutableSet<IRNode>>[] transferComponentSource(IRNode node) {
//      System.out.println("transferComponentSource:");
//      System.out.flush();
      
      // Initial state of affairs is no locks held
      ImmutableList<ImmutableSet<IRNode>>[] initValue = lattice.getEmptyValue();
      final IRNode flowUnit = lattice.getFlowUnit();
      
      for (HeldLock requiredLock : lattice.getRequiredLocks()) {
        final int idx = lattice.getIndexOf(requiredLock, thisExprBinder, binder);
        if (idx != -1) {
          // Push the lock precondition onto the stack as a new singleton set
          initValue = lattice.replaceValue(initValue, idx,
              lattice.getBaseLattice().push(initValue[idx],
                  ImmutableHashOrderSet.<IRNode>emptySet().addCopy(flowUnit)));
        }
      }
      
//      for (HeldLock stLock : lattice.getSingleThreaded()) {
//        final int idx = lattice.getIndexOf(stLock, thisExprBinder, binder);
//        if (idx != -1) {
//          // Push lock (from being a single-threaded constructor) onto the stack as a new singleton set
//          initValue = lattice.replaceValue(initValue, idx,
//              lattice.getBaseLattice().push(initValue[idx],
//                  ImmutableHashOrderSet.<IRNode>emptySet().addCopy(flowUnit)));
//        }
//      }
      
//      for (HeldLock ciLock : lattice.getClassInit()) {
//        final int idx = lattice.getIndexOf(ciLock, thisExprBinder, binder);
//        if (idx != -1) {
//          // Push lock (from being a class initializer) onto the stack as a new singleton set
//          initValue = lattice.replaceValue(initValue, idx,
//              lattice.getBaseLattice().push(initValue[idx],
//                  ImmutableHashOrderSet.<IRNode>emptySet().addCopy(flowUnit)));
//        }
//      }
      
//      System.out.println("  " + lattice.toString(initValue));
//      System.out.flush();
      return initValue;
    }
  }



  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<MustHoldLattice, ImmutableList<ImmutableSet<IRNode>>[]> {
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
    protected JavaForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice> realCreateAnalysis(
        final IRNode caller, final IBinder binder,
        final MustHoldLattice lattice,
        final ImmutableList<ImmutableSet<IRNode>>[] initialValue,
        final boolean terminationNormal) {
//    System.out.println("createAnalysis for " + DebugUnparser.toString(caller));
//    System.out.flush();
      return new JavaForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[], MustHoldLattice>(
          "Must Hold Analysis (sub-analysis)", lattice,
          new MustHoldTransfer(thisExprBinder, binder, lockUtils, lattice,
              query.getSubAnalysisQuery(caller)), DebugUnparser.viewer);
    }
    
  }
}

