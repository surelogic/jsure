package com.surelogic.analysis.concurrency.heldlocks.locks;

import java.util.Set;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

abstract class AbstractNeededLock extends AbstractILock implements NeededLock {
  /**
   * Create a new lock object.
   */
  AbstractNeededLock(
      final LockModel lm, final Type type) {
    super(lm, type);
  }
  
  @Override
  public final boolean isSatisfiedByLockSet(
      final Set<HeldLock> lockSet, final ThisExpressionBinder thisExprBinder) {
    for (final HeldLock current : lockSet) {
      if (current.mustSatisfy(this, thisExprBinder)) {
        return true;
      }
    }
    return false;
  }
}