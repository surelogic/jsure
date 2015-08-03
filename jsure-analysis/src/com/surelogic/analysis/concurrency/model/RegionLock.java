package com.surelogic.analysis.concurrency.model;

import com.surelogic.dropsea.ir.drops.locks.LockModel;

public final class RegionLock extends AbstractNamedLock<LockModel>{
  public RegionLock(final LockModel lockModel, final NamedLockImplementation lockImpl) {
    super(lockModel, lockImpl);
  }
  
  @Override
  public int hashCode() {
    return super.partialHashCode();
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof RegionLock) {
      return super.partialEquals((RegionLock) other);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "@PolicyLock(" + lockImpl + ")";
  }
}
