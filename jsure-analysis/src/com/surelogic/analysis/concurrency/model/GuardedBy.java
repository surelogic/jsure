package com.surelogic.analysis.concurrency.model;

import com.surelogic.dropsea.ir.drops.locks.GuardedByPromiseDrop;

/**
 * A lock from a GuardedBy field annotation.
 *
 */
public class GuardedBy extends AbstractModelLock<GuardedByPromiseDrop, UnnamedLockImplementation> {
  protected GuardedBy(
      final GuardedByPromiseDrop sourceDrop,
      final UnnamedLockImplementation lockImpl) {
    super(sourceDrop, lockImpl);
  }

  @Override
  public int hashCode() {
    return super.partialHashCode();
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof GuardedBy) {
      return super.partialEquals((GuardedBy) other);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "@GuardedBy(" + lockImpl + ")";
  }
}
