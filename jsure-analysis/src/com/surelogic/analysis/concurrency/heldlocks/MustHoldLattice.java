/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/MustHoldLattice.java,v 1.14 2008/01/19 00:14:21 aarong Exp $*/
package com.surelogic.analysis.concurrency.heldlocks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.concurrency.heldlocks.locks.HeldLock;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.uwm.cs.fluid.util.ListLattice;
import edu.uwm.cs.fluid.util.UnionLattice;

/**
 * Lattice for the Must-Hold analysis. Essentially a Map from final lock
 * expression IRNodes to a stack of sets of unlock call IRNodes. The map is
 * maintained as an ArrayLattice, where the lock expressions are mapped to array
 * locations. Specifically, the domain of the lattice is
 * {@code ImmutableList<ImmutableSet<IRNode>>[]}.
 * 
 * <p>
 * The meat of this class is implemented in {@link AbstractLockStackLattice}.
 * This class basically wraps the
 * {@link #peek(ImmutableList[], IRNode, IBinder)},
 * {@link #pushCall(ImmutableList[], IRNode, IBinder)}, and
 * {@link #popCall(ImmutableList[], IRNode, IBinder)} methods with the more
 * task-appropriate names
 * {@link #getLocksFor(ImmutableList[], IRNode, IBinder)},
 * {@link #foundLock(ImmutableList[], IRNode, IBinder)}, and
 * {@link #foundUnlock(ImmutableList[], IRNode, IBinder)}, respectively.
 * 
 * TODO: Say more about this.
 * 
 * @author aarong
 */
final class MustHoldLattice extends AbstractLockStackLattice {
  private final IRNode flowUnit;
  private final Set<HeldLock> requiredLocks;
  private final Set<HeldLock> singleThreaded;
  private final Set<HeldLock> classInit;
  
  /**
   * Private constructor. Use the factory method {@link #createForFlowUnit} to
   * create instances of this class.
   * 
   * @param lockExprs
   *          The list of unique lock expressions that represent the domain of
   *          the map portion of this lattice.
   */
  private MustHoldLattice(
      final IRNode fu, final ThisExpressionBinder teb, 
      final HeldLock[] locks, final Map<IRNode, Set<HeldLock>> map,
      final Set<HeldLock> req, final Set<HeldLock> st, final Set<HeldLock> ci) {
    super(teb, locks, map);
    flowUnit = fu;
    requiredLocks = req;
    singleThreaded = st;
    classInit = ci;
  }
  
  /**
   * Get an instance of this lattice that is suitable for use with the given
   * flow unit.
   * 
   * @param flowUnit
   *          The flow unit we need a lattice for.
   * @param binder
   *          The binder.
   * @return A new lattice instance that is customized based on the unique lock
   *         expressions present in the provided flow unit.
   */
  public static MustHoldLattice createForFlowUnit(
      final IRNode flowUnit, final ThisExpressionBinder thisExprBinder,
      final JUCLockUsageManager jucLockUsageManager) { 
    final Map<IRNode, Set<HeldLock>> map = jucLockUsageManager.getJUCLockExprsToLockSets(flowUnit);
    final Set<HeldLock> required = jucLockUsageManager.getJUCRequiredLocks(flowUnit);
    final Set<HeldLock> singleThreaded = jucLockUsageManager.getJUCSingleThreaded(flowUnit);
    final Set<HeldLock> classInit = jucLockUsageManager.getJUCClassInit(flowUnit);
    final HeldLock[] locks = constructLockArray(map, required, singleThreaded, classInit, thisExprBinder);
    return new MustHoldLattice(
        flowUnit, thisExprBinder, locks, map, required, singleThreaded, classInit);
  }
 
  
  
  /**
   * Push the given lock method call onto the stack.  The lock expression
   * is derived from the receiver of the method call.
   * 
   * @param oldValue
   * @param lockCall
   * @param binder
   * @return
   */
  public ImmutableList<ImmutableSet<IRNode>>[] foundLock(
      final ImmutableList<ImmutableSet<IRNode>>[] oldValue, final IRNode lockCall) {
    return pushCall(oldValue, lockCall);
  }

  /**
   * Remove the top lock method call based on the given unlock method call.
   * The lock expression is derived from the receiver of the method call.
   * 
   * @param oldValue
   * @param unlockCall
   * @param binder
   * @return
   */
  public ImmutableList<ImmutableSet<IRNode>>[] foundUnlock(
      final ImmutableList<ImmutableSet<IRNode>>[] oldValue, final IRNode unlockCall) {
    return popCall(oldValue, unlockCall);
  }
  
  /**
   * Get the the most recent set of lock calls for the given lock expression.
   * 
   * @return The set of lock calls at the top of stack for the given lock
   *         expression, or {@code null} if the top value is the poisoned TOP
   *         set value.
   */
  public Set<IRNode> getLocksFor(
      final ImmutableList<ImmutableSet<IRNode>>[] value, final IRNode lockExpr) {
    return peek(value, lockExpr);
  }
  
  /**
   * Get the set of lock expressions that are known to be locked, i.e., those
   * lock expressions in the domain of the lattice that have non-empty stacks.
   */
  public Set<HeldLock> getHeldLocks(
      final ImmutableList<ImmutableSet<IRNode>>[] value) {
    final Set<HeldLock> locked = new HashSet<HeldLock>();
    final ListLattice<UnionLattice<IRNode>, ImmutableSet<IRNode>> baseLattice = getBaseLattice();
    final ImmutableList<ImmutableSet<IRNode>> top = baseLattice.top();
    final ImmutableList<ImmutableSet<IRNode>> bottom = baseLattice.bottom();
    for (int i = 0; i < value.length; i++) {
      // skip bogus locks
      if (!(indices[i].isBogus())) {
        final ImmutableList<ImmutableSet<IRNode>> current = value[i];
        /* Bug 1010: Check if the list has a size > 1 because it will always have
         * the bogus element in it to differentiate the empty lattice value
         * from the bottom lattice value.
         */
        if (current != top && current != bottom && current.size() > 1) {
          /* TODO Need to retarget the locks here to have the correct source statements.
           * See the history of IntrinsicLockLattice.
           */
          locked.add(indices[i]);
        }
      }
    }
    return Collections.unmodifiableSet(locked);
  }
  
  public IRNode getFlowUnit() {
    return flowUnit;
  }
  
  public Set<HeldLock> getRequiredLocks() {
    return requiredLocks;
  }
  
  public Set<HeldLock> getSingleThreaded() {
    return singleThreaded;
  }
  
  public Set<HeldLock> getClassInit() {
    return classInit;
  }
}


