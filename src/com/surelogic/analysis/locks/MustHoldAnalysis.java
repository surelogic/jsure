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
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.uwm.cs.fluid.control.FlowAnalysis;
import edu.uwm.cs.fluid.control.ForwardAnalysis;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardTransfer;

public final class MustHoldAnalysis extends
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
  
  public MustHoldAnalysis(final ThisExpressionBinder teb, final IBinder b, final LockUtils lu,
      final JUCLockUsageManager lockMgr, final SimpleNonnullAnalysis sna) {
    super(b);
    thisExprBinder = teb;
    lockUtils = lu;
    jucLockUsageManager = lockMgr;
    nonNullAnalysis = sna;
    
  }

  private MustHoldLattice getLatticeFor(final IRNode node) {
    IRNode flowUnit = edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis.getFlowUnit(node);
    if (flowUnit == null)
      return null;
    final FlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[]> a = getAnalysis(flowUnit);
    final MustHoldLattice mhl = (MustHoldLattice) a.getLattice();
    return mhl;
  }

  @Override
  protected FlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[]> createAnalysis(final IRNode flowUnit) {
    final IRNode actualFlowUnit = (constructorContext == null) ? flowUnit : constructorContext;
    final MustHoldLattice mustHoldLattice =
      MustHoldLattice.createForFlowUnit(actualFlowUnit, thisExprBinder, binder, jucLockUsageManager);    
    final FlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[]> analysis =
      new ForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[]>(
          "Must Hold Analysis", mustHoldLattice,
          new MustHoldTransfer(thisExprBinder, binder, lockUtils, mustHoldLattice, nonNullAnalysis), DebugUnparser.viewer);
    return analysis;
  }

  /**
   * Given an unlock call, get the set of corresponding lock calls that it
   * might release. 
   * @return The IRNodes of the matching lock method call.
   */
  public Set<IRNode> getLocksFor(final IRNode mcall) {
    MethodCall call = (MethodCall) tree.getOperator(mcall);
    final ImmutableList<ImmutableSet<IRNode>>[] value = getAnalysisResultsBefore(mcall);
    final MustHoldLattice mhl = getLatticeFor(mcall);
    return mhl.getLocksFor(value, call.get_Object(mcall), thisExprBinder, binder);
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
    final MustHoldLattice mhl = getLatticeFor(node);
    return mhl.getHeldLocks(value);
  }
  
  private static final class MustHoldTransfer extends
      JavaForwardTransfer<MustHoldLattice, ImmutableList<ImmutableSet<IRNode>>[]> {
    private final ThisExpressionBinder thisExprBinder;
    private final LockUtils lockUtils;
    private final SimpleNonnullAnalysis nonNullAnalysis;
    
    public MustHoldTransfer(
        final ThisExpressionBinder teb, final IBinder binder, final LockUtils lu,
        final MustHoldLattice lattice, final SimpleNonnullAnalysis sna) {
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
    public ImmutableList<ImmutableSet<IRNode>>[] transferConditional(
        final IRNode node, final boolean flag, 
        final ImmutableList<ImmutableSet<IRNode>>[] before) {
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
            return lattice.foundUnlock(before, node, thisExprBinder, binder);
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
    protected ImmutableList<ImmutableSet<IRNode>>[] transferCall(
        final IRNode call, final boolean flag,
        final ImmutableList<ImmutableSet<IRNode>>[] value) {
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
    protected FlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[]> createAnalysis(IBinder binder) {
      return new ForwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[]>(
          "Must Hold Analysis", lattice, this, DebugUnparser.viewer);
    }

    public ImmutableList<ImmutableSet<IRNode>>[] transferComponentSource(IRNode node) {
      // Initial state of affairs is no locks held
      ImmutableList<ImmutableSet<IRNode>>[] initValue = lattice.getEmptyValue();
      
      for (HeldLock requiredLock : lattice.getRequiredLocks()) {
        final int idx = lattice.getIndexOf(requiredLock, thisExprBinder, binder);
        if (idx != -1) {
          // Push the lock precondition onto the stack as a new singleton set
          initValue = lattice.replaceValue(initValue, idx,
              lattice.getBaseLattice().push(initValue[idx],
                  ImmutableHashOrderSet.<IRNode>emptySet().addCopy(node)));
        }
      }
      
      for (HeldLock stLock : lattice.getSingleThreaded()) {
        final int idx = lattice.getIndexOf(stLock, thisExprBinder, binder);
        if (idx != -1) {
          // Push lock (from being a single-threaded constructor) onto the stack as a new singleton set
          initValue = lattice.replaceValue(initValue, idx,
              lattice.getBaseLattice().push(initValue[idx],
                  ImmutableHashOrderSet.<IRNode>emptySet().addCopy(node)));
        }
      }
      
      for (HeldLock ciLock : lattice.getClassInit()) {
        final int idx = lattice.getIndexOf(ciLock, thisExprBinder, binder);
        if (idx != -1) {
          // Push lock (from being a class initializer) onto the stack as a new singleton set
          initValue = lattice.replaceValue(initValue, idx,
              lattice.getBaseLattice().push(initValue[idx],
                  ImmutableHashOrderSet.<IRNode>emptySet().addCopy(node)));
        }
      }
      
      return initValue;
    }
  }
}

