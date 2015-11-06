package com.surelogic.analysis.concurrency.model;

import com.surelogic.dropsea.ir.PromiseDrop;

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
   * Get the PromiseDrop, if any, that supports the holding of this lock.
   * This is a PromiseDrop for a lock precondition or a single-threaded
   * constructor declaration.  This is <code>null</code> if not applicable
   * to this lock.
   */
  public PromiseDrop<?> getSupportingDrop();
}
