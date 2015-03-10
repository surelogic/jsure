package com.surelogic.analysis.concurrency.heldlocks;

import java.util.Set;

import com.surelogic.analysis.concurrency.heldlocks.locks.HeldLock;
import com.surelogic.dropsea.ir.drops.locks.ReturnsLockPromiseDrop;
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
import edu.cmu.cs.fluid.tree.Operator;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardTransfer;

public final class IntrinsicLockAnalysis extends
    edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis<Object[], IntrinsicLockLattice, JavaForwardAnalysis<Object[], IntrinsicLockLattice>> {
  public final class Query extends SimplifiedJavaFlowAnalysisQuery<Query, Set<HeldLock>, Object[], IntrinsicLockLattice> {
    public Query(final IThunk<? extends IJavaFlowAnalysis<Object[], IntrinsicLockLattice>> thunk) {
      super(thunk);
    }

    private Query(final Delegate<Query, Set<HeldLock>, Object[], IntrinsicLockLattice> d) {
      super(d);
    }

    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.ENTRY;
    }

    
    
    @Override
    protected Query newSubAnalysisQuery(final Delegate<Query, Set<HeldLock>, Object[], IntrinsicLockLattice> d) {
      return new Query(d);
    }

    @Override
    protected Set<HeldLock> processRawResult(
        final IRNode expr, IntrinsicLockLattice lattice, final Object[] rawResult) {
      return lattice.getHeldLocks(rawResult);
    }
  }
  

  
  private final LockUtils lockUtils;
  private final JUCLockUsageManager jucLockUsageManager;
  private final SimpleNonnullAnalysis nonNullAnalysis;
  
  public IntrinsicLockAnalysis(final IBinder b, final LockUtils lu,
      final JUCLockUsageManager lockMgr, final SimpleNonnullAnalysis sna) {
    super(b);
    lockUtils = lu;
    jucLockUsageManager = lockMgr;
    nonNullAnalysis = sna;
    
  }
  
  @Override
  protected JavaForwardAnalysis<Object[], IntrinsicLockLattice> createAnalysis(final IRNode flowUnit) {
    final IntrinsicLockLattice intrinsicLockLattice =
      IntrinsicLockLattice.createForFlowUnit(flowUnit, jucLockUsageManager);    
    final JavaForwardAnalysis<Object[], IntrinsicLockLattice> analysis =
      new JavaForwardAnalysis<Object[], IntrinsicLockLattice>(
        "Intrinsic Lock Analysis", intrinsicLockLattice,
        new IntrinsicLockTransfer(binder, lockUtils, intrinsicLockLattice,
            nonNullAnalysis.getNonnullBeforeQuery(flowUnit)),
            DebugUnparser.viewer);
    return analysis;
  }
  
//  /**
//   * Given a node, find the set of locks expressions that are locked on
//   * entry to the node.
//   */
//  public Set<HeldLock> getHeldLocks(final IRNode node, final IRNode context) {
//    final JavaForwardAnalysis<Object[], IntrinsicLockLattice> a = getAnalysis(
//        edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis.getFlowUnit(
//            node, context));
//    final IntrinsicLockLattice ill = a.getLattice();
//    return ill.getHeldLocks(a.getAfter(node, WhichPort.ENTRY));
//  }
  
  public Query getHeldLocksQuery(final IRNode flowUnit) {
    return new Query(getAnalysisThunk(flowUnit));
  }
  
  
  
  private static final class IntrinsicLockTransfer extends
      JavaForwardTransfer<IntrinsicLockLattice, Object[]> {
    private final LockUtils lockUtils;
    private final SimpleNonnullAnalysis.Query nonNullAnalysisQuery;

    
    
    public IntrinsicLockTransfer(final IBinder binder, final LockUtils lu,
        final IntrinsicLockLattice lattice, final SimpleNonnullAnalysis.Query query) {
      super(binder, lattice, new SubAnalysisFactory(lu, query));
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
    protected Object[] transferIsObject(IRNode node, boolean flag, Object[] value) {
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
                  lockUtils.isJUCRWMethod(initValue)) {
                return lattice.bottom();
              }
            }
          }
        } else if (MethodCall.prototype.includes(operator)) {
          if (isLockGetterMethod(node) || lockUtils.isJUCRWMethod(node)) {
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
    protected Object[] transferMonitorAction(
        final IRNode syncBlock, final boolean entering,
        final Object[] value) {
      if (entering) {
        return lattice.enteringSyncBlock(value, syncBlock);
      } else {
        return lattice.leavingSyncBlock(value, syncBlock);
      }
    }

    @Override
    public Object[] transferComponentSource(IRNode node) {
      // Initial state of affairs is no locks held
      return lattice.getEmptyValue();
    }
  }


 
  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<IntrinsicLockLattice, Object[]> {
    private final LockUtils lockUtils;
    private final SimpleNonnullAnalysis.Query query;
    
    public SubAnalysisFactory(
        final LockUtils lu, final SimpleNonnullAnalysis.Query q) {
      lockUtils = lu;
      query = q;
    }
   
    @Override
    protected JavaForwardAnalysis<Object[], IntrinsicLockLattice> realCreateAnalysis(
        final IRNode caller, final IBinder binder,
        final IntrinsicLockLattice lattice, final Object[] initialValue,
        final boolean terminationNormal) {
      return new JavaForwardAnalysis<Object[], IntrinsicLockLattice>(
          "Intrinsic Lock Analysis", lattice,
          new IntrinsicLockTransfer(binder, lockUtils, lattice,
              query.getSubAnalysisQuery(caller)), DebugUnparser.viewer);
    }
    
  }
}
