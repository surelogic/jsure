package com.surelogic.analysis.concurrency.heldlocks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.analysis.concurrency.heldlocks.locks.HeldLock;
import com.surelogic.common.ref.IJavaRef;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.SynchronizedStatement;
import edu.uwm.cs.fluid.util.AssociativeArrayLattice;
import edu.uwm.cs.fluid.util.FlatLattice;

/**
 * Lattice for tracking intrinsic locks via control-flow analysis.  The 
 * lattice is an array of flags.  Each array element corresponds to a specific
 * synchronized statement in the method being analyzed. The flag indicates
 * whether the synchronized statement is active or not.  We can use this
 * simplistic structure because of the syntactic constraints on 
 * synchronized blocks.
 */
final class IntrinsicLockLattice extends
    AssociativeArrayLattice<IRNode, FlatLattice, Object> {
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

  
  
  /* We don't need representation tricks to differentiate values from 
   * top and bottom like we do for AbstractLockStackLattice because here 
   * the underlying FlatLattice uses unique objects to represent top and
   * bottom, and these are not going to be equal to our LatticeValues.LOCKED
   * and LatticeValues.UNLOCKED values.
   * 
   * AbstractLockStackLattice needs to deal with bogus elements because its
   * underlying lattice is a List, and the bottom value there is the empty list.
   * So we need to make sure in that case that the "empty list" is not confused
   * with a bottom value.
   * 
   * Similarly we play tricks in the BindingContext lattice representation.
   */
  
  
  
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
//    super(FlatLattice.prototype, SLUtility.EMPTY_OBJECT_ARRAY, sb);
    super(FlatLattice.prototype, sb);
    locks = l;
    assumedLocks = assumed;
  }
  
  @Override
  protected Object[] newArray() {
    return new Object[size];
  }
  
  public static IntrinsicLockLattice createForFlowUnit(
      final IRNode flowUnit, final JUCLockUsageManager jucLockUsageManager) {
    final Map<IRNode, Set<HeldLock>> map = jucLockUsageManager.getSyncBlocks(flowUnit);
    final IRNode[] syncBlocks = new IRNode[map.keySet().size()];
    @SuppressWarnings("unchecked")
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
  
  
  
  // ======================================================================
  // == Associative array methods
  // ======================================================================
  
  @Override
  protected boolean indexEquals(final IRNode sb1, final IRNode sb2) {
    return sb1.equals(sb2);
  }
  
  /**
   * Get a new empty value the lattice: All the synchronized blocks are inactive.
   */
  @Override
  public final Object[] getEmptyValue() {
    final Object[] empty = new Object[size];
    for (int i = 0; i < size; i++) empty[i] = LatticeValues.UNLOCKED;
    return empty;
  }
  
  /**
   * Do we have a value that is not the bottom or the top value of the
   * lattice?
   */
  public boolean isNormal(final Object[] value) {
    return value != bottom() && value != top();
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
    return replaceValue(oldValue, syncBlock, status);
  }

  /**
   * Get the held locks.
   */
  public Set<HeldLock> getHeldLocks(final Object[] value) {
    final Set<HeldLock> result = new HashSet<HeldLock>(assumedLocks);
    for (int i = 0; i < size; i++) {
      if (value[i] == LatticeValues.LOCKED) result.addAll(locks[i]);
    }    
    return result;
  }
  

  
  @Override protected String toStringPrefixSeparator() { return "\n"; }
  @Override protected String toStringOpen() { return ""; }
  @Override protected String toStringSeparator() { return "\n"; }
  @Override protected String toStringConnector() { return " is "; }
  @Override protected String toStringClose() { return "\n"; }

  @Override
  protected void indexToString(final StringBuilder sb, final IRNode syncBlock) {
    sb.append("synchronized(");
    sb.append(DebugUnparser.toString(SynchronizedStatement.getLock(syncBlock)));
    sb.append(")@");
    final IJavaRef javaRef = JavaNode.getJavaRef(syncBlock);
    if (javaRef == null || javaRef.getLineNumber() == -1)
      sb.append("unknown");
    else {
      sb.append(javaRef.getLineNumber());
    }
  }
  
  @Override
  protected void valueToString(final StringBuilder sb, final Object value) {
    sb.append(value.toString());
  }
}
