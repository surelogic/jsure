/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/MustHoldLattice.java,v 1.14 2008/01/19 00:14:21 aarong Exp $*/
package com.surelogic.analysis.locks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.locks.locks.HeldLock;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.SynchronizedStatement;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.uwm.cs.fluid.util.ListLattice;
import edu.uwm.cs.fluid.util.UnionLattice;

/**
 * Lattice for the intrinsic lock analysis.  Essentially a Map from final lock
 * expression IRNodes to a stack of sets of synchronized block IRNodes. The map is
 * maintained as an ArrayLattice, where the lock expressions are mapped to array
 * locations. Specifically, the domain of the lattice is
 * {@code ImmutableList<ImmutableSet<IRNode>>[]}.
 * 
 * <p>
 * The meat of this class is implemented in {@link AbstractLockStackLattice}.
 * This class basically wraps the
 * {@link #pushLockExpression(ImmutableList[], IRNode, IRNode, ThisExpressionBinder, IBinder)} and
 * {@link #popLockExpression(ImmutableList[], IRNode, ThisExpressionBinder, IBinder)} methods with the more
 * task-appropriate names
 * {@link #enterSynchronized(ImmutableList[], IRNode, IBinder)}, and
 * {@link #leaveSynchronized(ImmutableList[], IRNode, IBinder)}, respectively. 
 */
final class IntrinsicLockLattice extends AbstractLockStackLattice {
  private final Set<HeldLock> syncMethodLocks;
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
  private IntrinsicLockLattice(
      final HeldLock[] locks, final Map<IRNode, Set<HeldLock>> map,
      final Set<HeldLock> sync, final Set<HeldLock> req,
      final Set<HeldLock> st, final Set<HeldLock> ci) {
    super(locks, map);
    syncMethodLocks = sync;
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
  public static IntrinsicLockLattice createForFlowUnit(
      final IRNode flowUnit, final ThisExpressionBinder thisExprBinder, final IBinder binder,
      final JUCLockUsageManager jucLockUsageManager) { 
    final Map<IRNode, Set<HeldLock>> map = jucLockUsageManager.getIntrinsicLockExprsToLockSets(flowUnit);
    final Set<HeldLock> sync = jucLockUsageManager.getIntrinsicSynchronizedMethodLocks(flowUnit);
    final Set<HeldLock> required = jucLockUsageManager.getIntrinsicRequiredLocks(flowUnit);
    final Set<HeldLock> singleThreaded = jucLockUsageManager.getIntrinsicSingleThreaded(flowUnit);
    final Set<HeldLock> classInit = jucLockUsageManager.getIntrinsicClassInit(flowUnit);
    final HeldLock[] locks = getLocksFromMap(map, thisExprBinder, binder);
    return new IntrinsicLockLattice(locks, map, sync, required, singleThreaded, classInit);
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
  public ImmutableList<ImmutableSet<IRNode>>[] enterSynchronized(
      final ImmutableList<ImmutableSet<IRNode>>[] oldValue,
      final IRNode syncBlock, final ThisExpressionBinder thisExprBinder, final IBinder binder) {
    return pushLockExpression(oldValue, SynchronizedStatement.getLock(syncBlock), syncBlock, thisExprBinder, binder);
  }

  /**
   * Remove the top lock method call based on the given unlock method call.
   * The lock expression is derived from the receiver of the method call.
   * 
   * @param oldValue
   * @param syncBlock
   * @param binder
   * @return
   */
  public ImmutableList<ImmutableSet<IRNode>>[] leaveSynchronized(
      final ImmutableList<ImmutableSet<IRNode>>[] oldValue,
      final IRNode syncBlock, final ThisExpressionBinder thisExprBinder, final IBinder binder) {
    return popLockExpression(oldValue, SynchronizedStatement.getLock(syncBlock), thisExprBinder, binder);
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
      if (!(locks[i].isBogus())) {
        final ImmutableList<ImmutableSet<IRNode>> current = value[i];
        /* Bug 1010: Check if the list has a size > 1 because it will always have
         * the bogus element in it to differentiate the empty lattice value
         * from the bottom lattice value.
         */
        if (current != top && current != bottom && current.size() > 1) {
          for (final ImmutableSet<IRNode> set : current) {
            for (final IRNode src : set) {
              if (src != IGNORE_ME) {
                locked.add(locks[i].changeSource(src));
              }
            }
          }
//          locked.add(locks[i]);
        }
      }
    }
    locked.addAll(syncMethodLocks);
    locked.addAll(requiredLocks);
    locked.addAll(singleThreaded);
    locked.addAll(classInit);
    return Collections.unmodifiableSet(locked);
  }
}


