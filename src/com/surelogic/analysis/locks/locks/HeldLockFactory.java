
/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/locks/HeldLockFactory.java,v 1.12 2009/02/17 14:01:32 aarong Exp $*/
package com.surelogic.analysis.locks.locks;

import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.analysis.ThisExpressionBinder;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;

public final class HeldLockFactory {
  private final ThisExpressionBinder thisBinder;
  
  public HeldLockFactory(final ThisExpressionBinder teb) {
    thisBinder = teb;
  }

  private void checkForIntrinsic(final LockModel ld) {
    if (ld.isJUCLock() || 
        ld.isReadWriteLock()) {
      throw new IllegalStateException("Expecting an intrinsic lock, but the lock is a JUC Lock or a JUC ReadWriteLock");
    }
  }

  private void checkForJUC(final LockModel ld) {
    if (!ld.isJUCLock()) {
      throw new IllegalStateException("Expecting a JUC Lock, but the lock is a JUC ReadWriteLock or an intrinsic lock");
    }
  }

  private void checkForReadWrite(final LockModel ld) {
    if (!ld.isReadWriteLock()) {
      throw new IllegalStateException("Expecting a JUC ReadWriteLock, but the lock is a JUC Lock or an intrinsic lock");      
    }
  }

  
  
  public HeldLock createIntrinsicInstanceLock(
      final IRNode o, final LockModel ld, final IRNode src,
      final PromiseDrop<?> sd, final boolean isAssumed) {
    checkForIntrinsic(ld);
    return new IRHeldInstanceLock(
        thisBinder.bindThisExpression(o), ld, src, sd, isAssumed, true, false);
  }

  public HeldLock createJUCInstanceLock(
      final IRNode o, final LockModel ld, final IRNode src,
      final PromiseDrop<?> sd, final boolean isAssumed) {
    checkForJUC(ld);
    return new IRHeldInstanceLock(
        thisBinder.bindThisExpression(o), ld, src, sd, isAssumed, true, false);
  }
  
  public HeldLock createJUCRWInstanceLock(
      final IRNode o, final LockModel ld, final IRNode src,
      final PromiseDrop<?> sd, final boolean isAssumed, final boolean isWrite) {
    checkForReadWrite(ld);
    return new IRHeldInstanceLock(thisBinder.bindThisExpression(o), ld, src, sd, isAssumed, isWrite, true);
  }
  
  public HeldLock createInstanceLock(
      final IRNode o, final LockModel ld, final IRNode src,
      final PromiseDrop<?> sd, final boolean isAssumed, final boolean isWrite) {
    if (ld.isReadWriteLock()) {
      return new IRHeldInstanceLock(thisBinder.bindThisExpression(o), ld, src, sd, isAssumed, isWrite, true);
    } else {
      return new IRHeldInstanceLock(thisBinder.bindThisExpression(o), ld, src, sd, isAssumed, true, false);
    }
  }
  
  public HeldLock createInstanceLock(
      final ExpressionNode o, final LockModel ld, final IRNode src,
      final PromiseDrop<?> sd, final boolean isAssumed, final boolean isWrite) {
    if (ld.isReadWriteLock()) {
      return new AASTHeldInstanceLock(o, ld, src, sd, isAssumed, isWrite, true);
    } else {
      return new AASTHeldInstanceLock(o, ld, src, sd, isAssumed, true, false);
    }
  }

  
  
  public HeldLock createIntrinsicStaticLock(
      final LockModel ld, final IRNode src, final boolean isAssumed) {
    checkForIntrinsic(ld);
    return new HeldStaticLock(ld, src, null, isAssumed, true, false);
  }
  
  public HeldLock createJUCStaticLock(
      final LockModel ld, final IRNode src, final boolean isAssumed) {
    checkForJUC(ld);
    return new HeldStaticLock(ld, src, null, isAssumed, true, false);
  }

  public HeldLock createJUCRWStaticLock(
      final LockModel ld, final IRNode src,
      final boolean isAssumed, final boolean isWrite) {
    checkForReadWrite(ld);
    return new HeldStaticLock(ld, src, null, isAssumed, isWrite, true);
  }

  public HeldLock createStaticLock(
      final LockModel ld, final IRNode src, final PromiseDrop<?> sd,
      final boolean isAssumed, final boolean isWrite) {
    if (ld.isReadWriteLock()) {
      return new HeldStaticLock(ld, src, sd, isAssumed, isWrite, true);
    } else {
      return new HeldStaticLock(ld, src, sd, isAssumed, true, false);
    }
  }

  /**
   * Factory method to create BogusLocks. This ensures that the expression
   * object is properly passed through a ThisExpressionBinder.
   * 
   * @param lockExpr
   *          The expression that is being used as a lock, but that doesn't
   *          correspond to a named lock.
   */
  public BogusLock createBogusLock(final IRNode lockExpr) {
    return new BogusLock(thisBinder.bindThisExpression(lockExpr));
  }
}
