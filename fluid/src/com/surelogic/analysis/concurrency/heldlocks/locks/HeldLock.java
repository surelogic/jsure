package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * Interface for objects that represent locks that are held by analysis, i.e.,
 * as a result of synchronized statements, lock preconditions, or lock() calls.
 * The held lock is identified by the node of the lock declaration.
 * 
 * <p>A lock also includes a pointer back to the statement that acquires
 * or returns the lock, or the expression that requires the lock.  This
 * "source" node is either
 * <ul>
 *   <li>A SynchronizedStatement IRNode if the lock was acquired in a
 *       <code>synchronized</code> statement
 *   <li>A MethodDeclaration IRNode if the lock
 *       was acquired because the method is <code>synchronized</code>, or
 *       if the lock is assumed to be acquired because of requires lock preconditions.
 *   <li>A MethodCall IRNode if the lock was acquired due to a {@code lock()}
 *       call.
 * </ul>
 */
public interface HeldLock extends ILock {
  public HeldLock changeSource(IRNode src);

  /**
   * Get the IRNode representing the syntactic entity that acquires,
   * requires, or returns the lock.
   */
  public IRNode getSource();
  
  /**
   * Get the PromiseDrop, if any, that supports the holding of this lock.
   * This is a PromiseDrop for a lock precondition or a single-threaded
   * constructor declaration.  This is <code>null</code> if not applicable
   * to this lock.
   */
  public PromiseDrop<?> getSupportingDrop();
  
  /**
   * Query if the lock must be identical to another lock.
   */  
  public boolean mustAlias(HeldLock lock, ThisExpressionBinder teb, IBinder b);
  
  /**
   * Query if the lock satisfies the holding of the other lock.
   */
  public boolean mustSatisfy(NeededLock lock, ThisExpressionBinder teb, IBinder b);
  
  /**
   * Is the lock assumed to be held because of a lock precondition.  If 
   * <code>false</code> the then lock has been explicitly acquired within
   * the body the method.
   */
  public boolean isAssumed();
  
  /**
   * Is the lock a "bogus lock", that is, a lock that is associated with a 
   * lock expression, but that is not mappable to a user-declared lock.  These
   * locks are used, for example, by the MustHold and MustRelease control-flow
   * analysis to match up lock() and unlock() calls even if the lock cannot
   * be determined to be a user-declared lock.
   */
  public boolean isBogus();
}
