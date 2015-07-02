package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.aast.promise.AbstractLockDeclarationNode;
import com.surelogic.aast.promise.LockSpecificationNode;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

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
 * 
 * @author aarong
 */
final class BogusLock implements HeldLock {
  /** The lock expression wrapped by this lock object. */
  private final IRNode lockExpr;
  
  
  
  BogusLock(final IRNode le) {
    lockExpr = le;
  }
  
  @Override
  public HeldLock changeSource(final IRNode newSrc) {
    return this;
  }

  @Override
  public boolean isBogus() {
    return true;
  }
  
  /**
   * Returns {@code null} because this is not a real lock and so it doesn't 
   * have a lock declaration.
   */
  @Override
  public AbstractLockDeclarationNode getLockDecl() {
    return null;
  }
  
  /**
   * Returns {@code null} because this is not a real lock and so it doesn't 
   * have a lock promise
   */
  @Override
  public LockModel getLockPromise() {
    return null;
  }

  @Override
  public String getName() {
    return "BogusLock";
  }

  @Override
  public IRNode getSource() {
    return lockExpr;
  }
  
  @Override
  public PromiseDrop<?> getSupportingDrop() {
    return null;
  }
  
  public LockSpecificationNode getSourceSpec() {
    return null;
  }
  
  @Override
  public boolean mustAlias(
      final HeldLock lock, final ThisExpressionBinder teb) {
    if (lock instanceof BogusLock) {
      return AbstractILock.checkSyntacticEquality(this.lockExpr, ((BogusLock) lock).lockExpr, teb);
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
  public boolean isAssumed() {
    return false;
  }
  
  @Override
  public boolean isWrite() {
    // This doesn't matter for bogus locks
    return false;
  }
}
