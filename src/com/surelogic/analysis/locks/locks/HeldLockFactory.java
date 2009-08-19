
/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/locks/HeldLockFactory.java,v 1.12 2009/02/17 14:01:32 aarong Exp $*/
package com.surelogic.analysis.locks.locks;

import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.locks.locks.ILock.Type;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;

public final class HeldLockFactory {
  private final ThisExpressionBinder thisBinder;
  
  public HeldLockFactory(final ThisExpressionBinder teb) {
    thisBinder = teb;
  }
  
  public HeldLock createInstanceLock(
      final IRNode o, final LockModel ld, final IRNode src,
      final PromiseDrop<?> sd, final boolean isAssumed, final Type type) {
    return new IRHeldInstanceLock(
        thisBinder.bindThisExpression(o), ld, src, sd, isAssumed, type);
  }

  public HeldLock createInstanceLock(
      final ExpressionNode o, final LockModel ld, final IRNode src,
      final PromiseDrop<?> sd, final boolean isAssumed, final Type type) {
    return new AASTHeldInstanceLock(o, ld, src, sd, isAssumed, type);

//    if (ld.isReadWriteLock()) {
//      return new AASTHeldInstanceLock(o, ld, src, sd, isAssumed, isWrite, true);
//    } else {
//      return new AASTHeldInstanceLock(o, ld, src, sd, isAssumed, true, false);
//    }
  }

  
  
  public HeldLock createStaticLock(
      final LockModel ld, final IRNode src, final PromiseDrop<?> sd,
      final boolean isAssumed, final Type type) {
    return new HeldStaticLock(ld, src, sd, isAssumed, type);
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
