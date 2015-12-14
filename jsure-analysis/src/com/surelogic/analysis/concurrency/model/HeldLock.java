package com.surelogic.analysis.concurrency.model;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.drops.locks.RequiresLockPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * A lock that is held by current thread of control. These are instantiations of
 * {@link ModelLock}s: in particular, for non-static locks, they are associated
 * with an expression that represents that object whose lock must be held.
 *
 * <p>
 * These work in concert with {@link NeededLock} objects that are instantiations
 * of {@link ModelLocks}s that represent locks that need to be held before a
 * particular region can be accessed. For analysis to be satisfied every
 * NeededLock must be matched to a HeldLock.
 * 
 * <p>
 * As these arise from locks being acquired in the code, they are associated
 * with a parse tree node that indicates where the lock is acquired.
 */
public interface HeldLock extends InstantiatedLock {
  /**
   * Get the parse tree node of the expression or declaration that acquires
   * the lock.
   */
  @Override
  public IRNode getSource();
  
  /**
   * Is a write lock held?
   */
  public boolean holdsWrite();
  
  /**
   * Get the RequiresLockDrop, if any, that supports the holding of this lock,
   * or <code>null</code> if the lock doesn't come from a lock precondition.
   */
  public RequiresLockPromiseDrop getSupportingDrop();

  /**
   * Query if the lock must be identical to another lock.
   */  
  public boolean mustAlias(HeldLock lock, ThisExpressionBinder teb);
  
  /**
   * Query if the lock satisfies the holding of the other lock.
   */
  public boolean mustSatisfy(NeededLock lock, ThisExpressionBinder teb);
}
