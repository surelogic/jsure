/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/MustReleaseLattice.java,v 1.15 2008/01/19 00:14:21 aarong Exp $*/
package com.surelogic.analysis.concurrency.heldlocks_new;

import java.util.Map;
import java.util.Set;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLock;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.ImmutableSet;

/**
 * Lattice for the Must-Release analysis. Essentially a Map from final lock
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
 * {@link #getUnlocksFor(ImmutableList[], IRNode, IBinder)},
 * {@link #foundUnlock(ImmutableList[], IRNode, IBinder)}, and
 * {@link #foundLock(ImmutableList[], IRNode, IBinder)}, respectively.
 * 
 * TODO: Say more about this.
 */
final class MustReleaseLattice extends AbstractLockStackLattice {
  /**
   * Private constructor. Use the factory method {@link #createForFlowUnit} to
   * create instances of this class.
   * 
   * @param lockExprs
   *          The list of unique lock expressions that represent the domain of
   *          the map portion of this lattice.
   */
  private MustReleaseLattice(
      final ThisExpressionBinder teb, 
      final HeldLock[] locks, final Map<IRNode, Set<HeldLock>> map) {
    super(teb, locks, map);
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
  public static MustReleaseLattice createForFlowUnit(
      final IRNode flowUnit, final ThisExpressionBinder thisExprBinder, 
      final LockExpressionManager lockExprManager) {    
    final Map<IRNode, Set<HeldLock>> map = lockExprManager.getFinalJUCLockExprs(flowUnit);
    return new MustReleaseLattice(
        thisExprBinder, getLocksFromMap(map, thisExprBinder), map);
  }
  
  
  
  /**
   * Push the given unlock method call onto the stack.  The lock expression
   * is derived from the receiver of the method call.
   * 
   * @param oldValue
   * @param unlockCall
   * @param binder
   * @return
   */
  public ImmutableList<ImmutableSet<IRNode>>[] foundUnlock(
      final ImmutableList<ImmutableSet<IRNode>>[] oldValue, final IRNode unlockCall) {
    return pushCall(oldValue, unlockCall);
  }

  /**
   * Remove the top unlock method call based on the given lock method call.
   * The lock expression is derived from the receiver of the method call.
   * 
   * @param oldValue
   * @param lockCall
   * @param binder
   * @return
   */
  public ImmutableList<ImmutableSet<IRNode>>[] foundLock(
      final ImmutableList<ImmutableSet<IRNode>>[] oldValue, final IRNode lockCall) {
    return popCall(oldValue, lockCall);
  }
  
  /**
   * Get the the most recent set of unlock calls for the given lock expression.
   * 
   * @return The set of unlock calls at the top of stack for the given lock
   *         expression, or {@code null} if the top value is the poisoned TOP
   *         set value.
   */
  public Set<IRNode> getUnlocksFor(
      final ImmutableList<ImmutableSet<IRNode>>[] value, final IRNode lockExpr) {
    return peek(value, lockExpr);
  }
}


