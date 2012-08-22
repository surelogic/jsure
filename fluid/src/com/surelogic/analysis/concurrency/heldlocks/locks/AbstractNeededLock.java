package com.surelogic.analysis.concurrency.heldlocks.locks;

import java.util.Set;

import com.surelogic.analysis.ThisExpressionBinder;

import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;


abstract class AbstractNeededLock extends AbstractILock implements NeededLock {
  /**
   * Create a new lock object.
   */
  AbstractNeededLock(
      final LockModel lm, final Type type) {
    super(lm, type);
  }
  
  public final boolean isSatisfiedByLockSet(final Set<HeldLock> lockSet,
      final ThisExpressionBinder thisExprBinder, final IBinder binder) {
    for (final HeldLock current : lockSet) {
      if (current.mustSatisfy(this, thisExprBinder, binder)) {
        return true;
      }
    }
    return false;
  }
}