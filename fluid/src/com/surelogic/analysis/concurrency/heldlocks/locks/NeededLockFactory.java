
/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/locks/NeededLockFactory.java,v 1.7 2008/01/19 00:14:21 aarong Exp $*/
package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.concurrency.heldlocks.locks.ILock.Type;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;

public final class NeededLockFactory {
  private final ThisExpressionBinder thisBinder;
  
  public NeededLockFactory(final ThisExpressionBinder teb) {
    thisBinder = teb;
  }

  public NeededLock createInstanceLock(
      final IRNode o, final LockModel lm, final Type type) {
    return new NeededInstanceLock(thisBinder.bindThisExpression(o), lm, type);
  } 
  
  public NeededLock createStaticLock(
      final LockModel lm, final Type type) {
    return new NeededStaticLock(lm, type);
  }
}
