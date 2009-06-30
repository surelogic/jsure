package com.surelogic.analysis.locks.locks;

import java.util.Set;

import com.surelogic.analysis.ThisExpressionBinder;

import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;


abstract class AbstractNeededLock extends AbstractILock implements NeededLock {
  /**
   * Create a new lock object.
   * 
   * @param ld
   *          The lock declaration node of the lock in question
   * @param src
   *          The node that is referring to the lock. See the class description.
   */
  AbstractNeededLock(
      final LockModel ld, final boolean write, final boolean rw) {
    super(ld, write, rw);
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