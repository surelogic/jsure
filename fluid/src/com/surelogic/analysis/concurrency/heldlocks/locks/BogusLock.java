/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/locks/BogusLock.java,v 1.13 2009/02/17 14:01:32 aarong Exp $*/
package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.aast.promise.AbstractLockDeclarationNode;
import com.surelogic.aast.promise.LockSpecificationNode;
import com.surelogic.analysis.ThisExpressionBinder;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;

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
  
  public HeldLock changeSource(final IRNode newSrc) {
    return this;
  }

  public boolean isBogus() {
    return true;
  }
  
  /**
   * Returns {@code null} because this is not a real lock and so it doesn't 
   * have a lock declaration.
   */
  public AbstractLockDeclarationNode getLockDecl() {
    return null;
  }
  
  /**
   * Returns {@code null} because this is not a real lock and so it doesn't 
   * have a lock promise
   */
  public LockModel getLockPromise() {
    return null;
  }

  public String getName() {
    return "BogusLock";
  }

  public IRNode getSource() {
    return lockExpr;
  }
  
  public PromiseDrop<?> getSupportingDrop() {
    return null;
  }
  
  public LockSpecificationNode getSourceSpec() {
    return null;
  }
  
  public boolean mustAlias(
      final HeldLock lock, final ThisExpressionBinder teb, final IBinder b) {
    if (lock instanceof BogusLock) {
      return AbstractHeldLock.checkSyntacticEquality(this.lockExpr, ((BogusLock) lock).lockExpr, teb, b);
    } else {
      return false;
    }
  }
  
  public boolean mustSatisfy(
      final NeededLock lock, final ThisExpressionBinder teb, final IBinder b) {
    return false;
  }

  public boolean isAssumed() {
    return false;
  }
  
  public boolean isWrite() {
    // This doesn't matter for bogus locks
    return false;
  }
}
