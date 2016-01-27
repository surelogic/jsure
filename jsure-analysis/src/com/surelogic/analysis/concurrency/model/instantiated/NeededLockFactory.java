package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.aast.IAASTNode;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.analysis.concurrency.model.instantiated.NeededLock.Reason;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Factory class for creating NeededLock objects.  This class exists as a way
 * to force the object expressions for instance locks to be run through a 
 * ThisExpressionBinder instance.
 * 
 */
public final class NeededLockFactory {
  private final ThisExpressionBinder thisExprBinder;
  
  public NeededLockFactory(final ThisExpressionBinder thisExprBinder) {
    this.thisExprBinder = thisExprBinder;
  }
  
  
  
  public NeededLock createInstanceLock(
      final IRNode objectRefExpr, final LockImplementation lockImpl,
      final IRNode source, final Reason reason,
      final PromiseDrop<? extends IAASTNode> assuredPromise, final boolean needsWrite) {
    return new NormalNeededInstanceLock(
        thisExprBinder.bindThisExpression(objectRefExpr), 
        lockImpl, source, reason, assuredPromise, needsWrite);
  }
    
  public NeededLock createStaticLock(
      final LockImplementation lockImpl, final IRNode source,
      final Reason reason, final PromiseDrop<? extends IAASTNode> assuredPromise,
      final boolean needsWrite) {
    return new NeededStaticLock(lockImpl, source, reason, assuredPromise, needsWrite);
  }
}
