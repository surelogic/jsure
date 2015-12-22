package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLock.Reason;
import com.surelogic.dropsea.ir.drops.locks.RequiresLockPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Factory class for creating HeldLock objects.  This class exists as a way
 * to force the object expressions for instance locks to be run through a 
 * ThisExpressionBinder instance.
 * 
 */
public final class HeldLockFactory {
  private final ThisExpressionBinder thisExprBinder;
  
  public HeldLockFactory(final ThisExpressionBinder thisExprBinder) {
    this.thisExprBinder = thisExprBinder;
  }
  
  
  
  public HeldLock createInstanceLock(
      final IRNode objectRefExpr, final LockImplementation lockImpl,
      final IRNode source, final Reason reason, final boolean needsWrite,
      final RequiresLockPromiseDrop supportingDrop) {
    return new HeldInstanceLock(
        thisExprBinder.bindThisExpression(objectRefExpr), 
        lockImpl, source, reason, needsWrite, supportingDrop);
  }
  
  public HeldLock createStaticLock(
      final LockImplementation lockImpl, final IRNode source,
      final Reason reason, final boolean needsWrite,
      final RequiresLockPromiseDrop supportingDrop) {
    return new HeldStaticLock(lockImpl, source, reason, needsWrite, supportingDrop);
  }
  
  public BogusLock createBogusLock(final IRNode lockExpr) {
    return new BogusLock(thisExprBinder.bindThisExpression(lockExpr));
  }
}
