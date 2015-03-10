package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;

abstract class AbstractHeldLock extends AbstractILock implements HeldLock {
  /**
   * Is the lock assumed to be held.
   */
  protected final boolean isAssumed;
  
  /**
   * The statement that acquires or returns the lock.
   */
  protected final IRNode srcExpr;

  /**
   * Promise drop for any supporting annotations. Links the correctness of
   * holding this lock with the assurance of the given annotation. If not
   * applicable to this lock, e.g., the lock is from a returns lock annotation,
   * it must be <code>null</code>.
   */
  protected final PromiseDrop<?> supportingDrop;
  
  
  
  /**
   * Create a new lock object.
   * 
   * @param lm
   *          The lock declaration node of the lock in question
   * @param src
   *          The node that is referring to the lock. See the class description.
   */
  AbstractHeldLock(final LockModel lm, final IRNode src,
      final PromiseDrop<?> sd, final boolean assumed, final Type type) {
    super(lm, type);
    isAssumed = assumed;
    srcExpr = src;
    supportingDrop = sd;
  }
  
  /**
   * Get the node that names the lock.
   */
  @Override
  public final IRNode getSource() {
    return srcExpr;
  }
  
  @Override
  public final PromiseDrop<?> getSupportingDrop() {
    return supportingDrop;
  }
  
  @Override
  public final boolean isAssumed() {
    return isAssumed;
  }

  @Override
  public final boolean isBogus() {
    return false;
  }
}
