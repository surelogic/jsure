/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/AbstractLockStackLattice.java,v 1.18 2008/01/19 00:14:21 aarong Exp $*/
package com.surelogic.analysis.locks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.analysis.locks.locks.HeldLock;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.SynchronizedStatement;
import edu.uwm.cs.fluid.util.ArrayLattice;
import edu.uwm.cs.fluid.util.FlatLattice;

/**
 * Lattice for tracking intrinsic locks via control-flow analysis.  The 
 * lattice is an array of flags.  Each array element corresponds to a specific
 * synchronized statement in the method being analyzed. The flag indicates
 * whether the synchronized statement is active or not.  We can use this
 * simplisitic structure because of the syntactic constraints on 
 * synchronized blocks.
 */
final class IntrinsicLockLattice extends ArrayLattice<FlatLattice, Object> {
  /**
   * Values used in the FlatLattice for tracking each individual 
   * synchronized statement.
   */
  private static enum LatticeValues { 
    /** Value indicating the synchronized statement is active. */
    LOCKED {
      @Override
      public String toString() { return "LOCKED"; }
    },
    
    /** Value indicating the synchronized statement is not active. */
    UNLOCKED {
      @Override
      public String toString() { return "UNLOCKED"; }
    }
  }

  
  
  /**
   * The list of synchronized blocks tracked by the lattice.
   */
  private final IRNode[] syncBlocks;
  
  /**
   * The set of of locks acquired by each synchronized block.
   */
  private final Set<HeldLock>[] locks;

  /**
   * Locks assumed to be held, that cannot be released during the execution 
   * of the method.  This come from the method being {@code synchronized},
   * from {@code RequiresLock} annotations, from a constructor being
   * single threaded, or from the special case of a class initializer.
   */
  private final Set<HeldLock> assumedLocks;

  
  
  
  /**
   * Private constructor: use {@link #createForFlowUnit(IRNode, JUCLockUsageManager)}.
   */
  private IntrinsicLockLattice(
      final IRNode[] sb, final Set<HeldLock>[] l, final Set<HeldLock> assumed) {
    super(FlatLattice.prototype, sb.length, new Object[0]);
    syncBlocks = sb;
    locks = l;
    assumedLocks = assumed;
  }
  
  @SuppressWarnings("unchecked")
  public static IntrinsicLockLattice createForFlowUnit(
      final IRNode flowUnit, final JUCLockUsageManager jucLockUsageManager) {
    final Map<IRNode, Set<HeldLock>> map = jucLockUsageManager.getSyncBlocks(flowUnit);
    final IRNode[] syncBlocks = new IRNode[map.keySet().size()];
    final Set<HeldLock>[] locks = new Set[syncBlocks.length];
    int i = 0;
    for (final Map.Entry<IRNode, Set<HeldLock>> entry : map.entrySet()) {
      syncBlocks[i] = entry.getKey();
      locks[i] = entry.getValue();
      i += 1;
    }
    
    final Set<HeldLock> assumedLocks = 
      jucLockUsageManager.getIntrinsicAssumedLocks(flowUnit);
    return new IntrinsicLockLattice(syncBlocks, locks, assumedLocks);
  }  
  
  /**
   * Get the array index of the given sync block
   * @return The index of the sync block in the array lattice, or
   *         {@code -1} if the sync block is not found in the lattice.
   */
  private int getIndexOf(final IRNode syncBlock) {
    for (int i = 0; i < syncBlocks.length; i++) {
      if (syncBlock == syncBlocks[i]) return i;
    }
    // Not found
    return -1;
  }
  
  /**
   * Mark the given synchronized block as being active.
   */
  public Object[] enteringSyncBlock(
      final Object[] oldValue, final IRNode syncBlock) {
    return updateSyncBlock(oldValue, syncBlock, LatticeValues.LOCKED);
  }
  
  /**
   * Mark the given synchronized block as being inactive.
   */
  public Object[] leavingSyncBlock(
      final Object[] oldValue, final IRNode syncBlock) {
    return updateSyncBlock(oldValue, syncBlock, LatticeValues.UNLOCKED);
  }
  
  /**
   * Update the status of the given synchronized block
   */
  private Object[] updateSyncBlock(
      final Object[] oldValue, final IRNode syncBlock, final LatticeValues status) {
    final int idx = getIndexOf(syncBlock);
    Object[] result = oldValue;
    if (idx != -1) {
      result = replaceValue(result, idx, status);
    }
    return result;
  }

  /**
   * Get the held locks.
   */
  public Set<HeldLock> getHeldLocks(final Object[] value) {
    final Set<HeldLock> result = new HashSet<HeldLock>(assumedLocks);
    for (int i = 0; i < syncBlocks.length; i++) {
      if (value[i] == LatticeValues.LOCKED) result.addAll(locks[i]);
    }    
    return result;
  }
  
  /**
   * Get a new empty value the lattice: All the synchronized blocks are inactive.
   */
  public final Object[] getEmptyValue() {
    final Object[] empty = new Object[syncBlocks.length];
    for (int i = 0; i < empty.length; i++) empty[i] = LatticeValues.UNLOCKED;
    return empty;
  }

  @Override
  public String toString(final Object[] value) {
    final StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (int i = 0; i < syncBlocks.length; i++) {
      sb.append("synchronized(");
      sb.append(DebugUnparser.toString(SynchronizedStatement.getLock(syncBlocks[i])));
      sb.append(")@");
      sb.append(JavaNode.getSrcRef(syncBlocks[i]).getLineNumber());
      sb.append("->");
      sb.append(value[i]);
      if (i != syncBlocks.length-1) { sb.append(' '); }
    }
    sb.append(']');
    return sb.toString();
  }
  
  /**
   * Do we have a value that is not the bottom or the top value of the
   * lattice?
   */
  public boolean isNormal(final Object[] value) {
    return value != bottom() && value != top();
  }
}
