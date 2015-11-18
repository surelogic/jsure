package com.surelogic.analysis.concurrency.model;

import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * Represents lock expressions that aren't mappable to any user-defined 
 * lock.  This is used by the control-flow analyses, and should never to be
 * used by LockVisitor.  The lock control flow analyses need to track the locks,
 * and because of locking pre- and postconditions need to operate in terms of 
 * locks and not lock expressions.  But we would like to be able to report 
 * nesting errors for lock expressions even if they don't correspond to any
 * user-declared locks.  For this case we use the BogusLock class so that we
 * have a Lock object.
 */
public final class BogusLock implements HeldLock {
  /** The lock expression wrapped by this lock object. */
  private final IRNode lockExpr;
  
    
  
  public BogusLock(final IRNode lockExpr) {
    this.lockExpr = lockExpr;
  }

  @Override
  public IRNode getSource() {
    return lockExpr;
  }

  @Override
  public boolean holdsWrite() {
    // Doesn't really matter for bogus locks
    return true;
  }

  @Override
  public PromiseDrop<?> getSupportingDrop() {
    // No promise supports a bogus lock because they aren't real locks
    return null;
  }
  
  @Override
  public final boolean isIntrinsic(final IBinder binder) {
    return false;
  }
  
  @Override
  public final boolean isJUC(final IBinder binder) {
    return false;
  }
  
  @Override
  public final boolean isReadWrite(final IBinder binder) {
    return false;
  }
}
