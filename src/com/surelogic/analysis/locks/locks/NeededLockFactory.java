
/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/locks/NeededLockFactory.java,v 1.7 2008/01/19 00:14:21 aarong Exp $*/
package com.surelogic.analysis.locks.locks;

import com.surelogic.analysis.ThisExpressionBinder;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;

/* Only need to know the name of the needed lock
 * and whether write privileges are necessary.  We don't have any intrinsic/juc 
 * requirement here.  We match with held locks by the lock declaration, which
 * implicitly determines whether a JUC or intrinsic lock is used.
 */
public final class NeededLockFactory {
  private final ThisExpressionBinder thisBinder;
  
  public NeededLockFactory(final ThisExpressionBinder teb) {
    thisBinder = teb;
  }

  public NeededLock createInstanceLock(
      final IRNode o, final LockModel ld, final boolean write) {
    return new NeededInstanceLock(thisBinder.bindThisExpression(o), ld, write, ld.isReadWriteLock());
  } 
  
  public NeededLock createStaticLock(
      final LockModel ld, final boolean write) {
    return new NeededStaticLock(ld, write, ld.isReadWriteLock());
  }
}
