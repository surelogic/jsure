package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.concurrency.model.SyntacticEquality;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.RequiresLockPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
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
  public int hashCode() {
    int result = 17;
    result += 31 * lockExpr.hashCode();
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) { 
      return true;
    } else if (other instanceof BogusLock) {
      final BogusLock o = (BogusLock) other;
      return this.lockExpr.equals(o.lockExpr);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "Lock expression " + DebugUnparser.toString(lockExpr);
  }

  
  
  /**
   * Check that the same lock is used, and then use syntactic equality of the
   * object expressions.
   */
  @Override
  public boolean mustAlias(final HeldLock lock, final ThisExpressionBinder teb) {
    if (lock instanceof BogusLock) {
      final BogusLock o = (BogusLock) lock;
      return SyntacticEquality.checkSyntacticEquality(lockExpr, o.lockExpr, teb);
    } else {
      return false;
    }
  }
  
  @Override
  public boolean mustSatisfy(
      final NeededLock lock, final ThisExpressionBinder teb) {
    return false;
  }

  @Override
  public IRNode getSource() {
    return lockExpr;
  }

  @Override
  public Reason getReason() {
    return Reason.BOGUS;
  }

  @Override
  public boolean holdsWrite() {
    // Doesn't really matter for bogus locks
    return true;
  }
  
  @Override
  public final PromiseDrop<?> getLockPromise() {
    return null;
  }

  @Override
  public RequiresLockPromiseDrop getSupportingPromise() {
    // No promise supports a bogus lock because they aren't real locks
    return null;
  }
  
  @Override
  public final boolean isStatic() {
    return false;
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
