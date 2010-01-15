package com.surelogic.analysis.locks;

import java.util.Set;

import com.surelogic.analysis.locks.locks.HeldLock;

import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.analysis.AnalysisQuery;
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
import edu.uwm.cs.fluid.control.ForwardAnalysis;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardTransfer;

public final class IntrinsicLockAnalysis extends
    edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis<Object[], IntrinsicLockLattice, IntrinsicLockAnalysis.Analysis> {
  public static interface Query extends AnalysisQuery<Set<HeldLock>> {
    // adds nothing
  }
  
  public static final class Analysis extends ForwardAnalysis<Object[], IntrinsicLockLattice, IntrinsicLockTransfer> {
    private Analysis(
        final String name, final IntrinsicLockLattice l, final IntrinsicLockTransfer t) {
      super(name, l, t, DebugUnparser.viewer);
    }
    
    public Analysis getSubAnalysis() {
      return trans.getSubAnalysis();
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
  protected Analysis createAnalysis(final IRNode flowUnit) {
    final IntrinsicLockLattice intrinsicLockLattice =
      IntrinsicLockLattice.createForFlowUnit(flowUnit, jucLockUsageManager);    
    final Analysis analysis = new Analysis(
        "Intrinsic Lock Analysis", intrinsicLockLattice,
        new IntrinsicLockTransfer(
            flowUnit, binder, lockUtils, intrinsicLockLattice, nonNullAnalysis));
    return analysis;
  }
  
  /**
   * Given a node, find the set of locks expressions that are locked on
   * entry to the node.
   */
  public Set<HeldLock> getHeldLocks(final IRNode node, final IRNode context) {
    final Analysis a = getAnalysis(
        edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis.getFlowUnit(
            node, context));
    final IntrinsicLockLattice ill = a.getLattice();
    return ill.getHeldLocks(a.getAfter(node, WhichPort.ENTRY));
  }
  
  public Query getHeldLocksQuery(final IRNode flowUnit, final boolean initializer) {
    return new Query() {
      private final Analysis a;
      {
        final Analysis t = getAnalysis(flowUnit);
        a = initializer ? t.getSubAnalysis() : t;
      }
      private final IntrinsicLockLattice lattice = a.getLattice();

      public Set<HeldLock> getResultFor(final IRNode expr) {
        return lattice.getHeldLocks(a.getAfter(expr, WhichPort.ENTRY));
      }
    };
  }
  
  
  
  private static final class IntrinsicLockTransfer extends
      JavaForwardTransfer<Analysis, IntrinsicLockLattice, Object[]> {
    private final LockUtils lockUtils;
    private final SimpleNonnullAnalysis.Query nonNullAnalysisQuery;
    
    /**
     * We cache the subanalysis we create so that both normal and abrupt paths
     * are stored in the same analysis. Plus this puts more force behind an
     * assumption made by
     * {@link JavaTransfer#runClassInitializer(IRNode, IRNode, T, boolean)}.
     * 
     * <p>
     * <em>Warning: reusing analysis objects won't work if we have smart worklists.</em>
     */
    private Analysis subAnalysis = null;

    
    
    public IntrinsicLockTransfer(final IRNode flowUnit,
        final IBinder binder, final LockUtils lu,
        final IntrinsicLockLattice lattice, final SimpleNonnullAnalysis sna) {
      super(binder, lattice);
      lockUtils = lu;
      nonNullAnalysisQuery = sna.getNonnullBeforeQuery(flowUnit);
    }
    
    
    
    public Analysis getSubAnalysis() {
      return subAnalysis;
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
    protected Analysis createAnalysis(
        final IBinder binder, final boolean terminationNormal) {
      if (subAnalysis == null) {
        subAnalysis = new Analysis("Intrinsic Lock Analysis", lattice, this);
      }
      return subAnalysis;
    }

    public Object[] transferComponentSource(IRNode node) {
      // Initial state of affairs is no locks held
      return lattice.getEmptyValue();
    }
  }
}
