package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.concurrency.model.declared.ModelLock;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLock.Reason;
import com.surelogic.dropsea.ir.PromiseDrop;
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
      final PromiseDrop<?> lockPromise,
      final RequiresLockPromiseDrop supportingDrop) {
    return new HeldInstanceLock(
        thisExprBinder.bindThisExpression(objectRefExpr), 
        lockImpl, source, reason, needsWrite, lockPromise, supportingDrop);
  }
  
  public HeldLock createInstanceLock(
      final IRNode objectRefExpr, final ModelLock<?, ?> modelLock,
      final IRNode source, final Reason reason, final boolean needsWrite,
      final RequiresLockPromiseDrop supportingDrop) {
    return new HeldInstanceLock(
        thisExprBinder.bindThisExpression(objectRefExpr), 
        modelLock.getImplementation(), source, reason, needsWrite,
        modelLock.getSourceAnnotation(), supportingDrop);
  }

  public HeldLock createFieldRefLock(
      final IRNode objectRefExpr, final IRNode varDecl,
      final LockImplementation lockImpl,
      final IRNode source, final Reason reason, final boolean needsWrite,
      final PromiseDrop<?> lockPromise,
      final RequiresLockPromiseDrop supportingDrop) {
    return new HeldFieldRefLock(
        thisExprBinder.bindThisExpression(objectRefExpr),  varDecl,
        lockImpl, source, reason, needsWrite, lockPromise, supportingDrop);
  }
  
  public HeldLock createInstanceParameterDeclLock(
      final IRNode paramDecl, final LockImplementation lockImpl,
      final IRNode source, final Reason reason, final boolean needsWrite,
      final PromiseDrop<?> lockPromise,
      final RequiresLockPromiseDrop supportingDrop) {
    return new HeldInstanceParamDeclLock(paramDecl, 
        lockImpl, source, reason, needsWrite, lockPromise, supportingDrop);
  }

  public HeldLock createStaticLock(
      final LockImplementation lockImpl, final IRNode source,
      final Reason reason, final boolean needsWrite,
      final PromiseDrop<?> lockPromise,
      final RequiresLockPromiseDrop supportingDrop) {
    return new HeldStaticLock(
        lockImpl, source, reason, needsWrite,lockPromise, supportingDrop);
  }
  
  public HeldLock createStaticLock(
      final ModelLock<?, ?> modelLock, final IRNode source,
      final Reason reason, final boolean needsWrite,
      final RequiresLockPromiseDrop supportingDrop) {
    return new HeldStaticLock(
        modelLock.getImplementation(), source, reason, needsWrite,
        modelLock.getSourceAnnotation(), supportingDrop);
  }
  
  public BogusLock createBogusLock(final IRNode lockExpr) {
    return new BogusLock(thisExprBinder.bindThisExpression(lockExpr));
  }
}
