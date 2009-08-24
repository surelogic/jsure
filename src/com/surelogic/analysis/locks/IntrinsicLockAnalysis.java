/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/MustHoldAnalysis.java,v 1.28 2008/04/30 20:55:48 aarong Exp $*/
package com.surelogic.analysis.locks;

import java.util.Set;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.locks.locks.HeldLock;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
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
import edu.uwm.cs.fluid.control.FlowAnalysis;
import edu.uwm.cs.fluid.control.ForwardAnalysis;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardTransfer;

public final class IntrinsicLockAnalysis extends
    edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis<ImmutableList<ImmutableSet<IRNode>>[]> {
  private final ThisExpressionBinder thisExprBinder;
  private final LockUtils lockUtils;
  private final JUCLockUsageManager jucLockUsageManager;
  private final SimpleNonnullAnalysis nonNullAnalysis;
  
  /**
   * This field is a horrible hack, but I don't know how else do solve the
   * problem. The problem is that I would like to analyze instance field
   * declarations, their associated initializers especially, and instance
   * initializer blocks in the context of a particular constructor. Flow
   * analysis doesn't support this. This is important when getting the held
   * locks because I need to take into account whether a constructor is single
   * threaded or not. Instance field declarations and instance initializers are
   * given an InitDeclaration node as their FlowUnit. This puts all the instance
   * initialization gobledygook in a single flow unit. Unfortunately, it is
   * stand alone. So when getting the held locks, the caller,
   * {@link LockVisitor}, provides the constructor that is of interest. This
   * constructor is stored here, to be used by {@link #createAnalysis(IRNode)}
   * instead of the normally provided flowUnit when initializing the lattice.
   * This way the lattice state is initialized from the constructor, picking up
   * whatever lock information is on the constructor.
   * 
   * <p>
   * Otherwise, this field is left {@code null}.
   */
  private IRNode constructorContext = null;
  
  public IntrinsicLockAnalysis(final ThisExpressionBinder teb, final IBinder b, final LockUtils lu,
      final JUCLockUsageManager lockMgr, final SimpleNonnullAnalysis sna) {
    super(b);
    thisExprBinder = teb;
    lockUtils = lu;
    jucLockUsageManager = lockMgr;
    nonNullAnalysis = sna;
    
  }

  private IntrinsicLockLattice getLatticeFor(final IRNode node) {
    IRNode flowUnit = edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis.getFlowUnit(node);
    if (flowUnit == null)
      return null;
    final FlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[]> a = getAnalysis(flowUnit);
    final IntrinsicLockLattice mhl = (IntrinsicLockLattice) a.getLattice();
    return mhl;
  }

  @Override
  protected FlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[]> createAnalysis(final IRNode flowUnit) {
    final IRNode actualFlowUnit = (constructorContext == null) ? flowUnit : constructorContext;
    final IntrinsicLockLattice intrinsicLockLattice =
      IntrinsicLockLattice.createForFlowUnit(actualFlowUnit, thisExprBinder, binder, jucLockUsageManager);    
    final FlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[]> analysis =
      new ForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[]>(
          "Must Hold Analysis", intrinsicLockLattice,
          new IntrinsicLockTransfer(thisExprBinder, binder, lockUtils, intrinsicLockLattice, nonNullAnalysis), DebugUnparser.viewer);
    return analysis;
  }
  
  /**
   * Given a node, find the set of locks expressions that are locked on
   * entry to the node.
   */
  public Set<HeldLock> getHeldLocks(final IRNode node, final IRNode context) {
    final ImmutableList<ImmutableSet<IRNode>>[] value;
    constructorContext = context;
    try {
      value = getAnalysisResultsBefore(node);
    } finally {
      constructorContext = null;
    }
    final IntrinsicLockLattice ill = getLatticeFor(node);
    System.out.println(ill.toString(value));
    return ill.getHeldLocks(value);
  }
  
  
  
  private static final class IntrinsicLockTransfer extends
      JavaForwardTransfer<IntrinsicLockLattice, ImmutableList<ImmutableSet<IRNode>>[]> {
    private final ThisExpressionBinder thisExprBinder;
    private final LockUtils lockUtils;
    private final SimpleNonnullAnalysis nonNullAnalysis;
    
    public IntrinsicLockTransfer(
        final ThisExpressionBinder teb, final IBinder binder, final LockUtils lu,
        final IntrinsicLockLattice lattice, final SimpleNonnullAnalysis sna) {
      super(binder, lattice);
      thisExprBinder = teb;
      lockUtils = lu;
      nonNullAnalysis = sna;
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
          final Set<IRNode> nonNull = nonNullAnalysis.getNonnullBefore(node);
          final IRNode varDecl = binder.getBinding(node);
          if (nonNull.contains(varDecl)) {
            return lattice.bottom();
          }
        }
      }
      return value;
    }

    @Override
    protected ImmutableList<ImmutableSet<IRNode>>[] transferMonitorAction(
        final IRNode syncBlock, final boolean entering,
        final ImmutableList<ImmutableSet<IRNode>>[] value) {
      if (entering) {
        return lattice.enterSynchronized(value, syncBlock, thisExprBinder, binder);
      } else {
        return lattice.leaveSynchronized(value, syncBlock, thisExprBinder, binder);
      }
    }
    
    @Override
    protected FlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[]> createAnalysis(IBinder binder) {
      return new ForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[]>(
          "Must Hold Analysis", lattice, this, DebugUnparser.viewer);
    }

    public ImmutableList<ImmutableSet<IRNode>>[] transferComponentSource(IRNode node) {
      // Initial state of affairs is no locks held
      return lattice.getEmptyValue();
    }
  }
}

