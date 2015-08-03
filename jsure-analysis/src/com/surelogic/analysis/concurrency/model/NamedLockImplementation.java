package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * A lock implementation assotiated with a name.  These always come
 * from PolicyLock and RegionLock.
 */
public final class NamedLockImplementation implements LockImplementation {
  /** The name of the lock. */
  private final String name;
  
  /** Delegate to the actual lock implementation. */
  private final LockImplementation lockImpl;
  
  
  
  public NamedLockImplementation(final String name, final LockImplementation lockImpl) {
    this.name = name;
    this.lockImpl = lockImpl;
  }
 
  
  
  @Override
  public String toString() {
    return name + " is " + lockImpl;
  }
  
  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + name.hashCode();
    result = 31 * result + lockImpl.hashCode();
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof NamedLockImplementation) {
      final NamedLockImplementation castOther = (NamedLockImplementation) other;
      return this.name.equals(castOther.name) && 
          this.lockImpl.equals(castOther.lockImpl);
    } else {
      return false;
    }
  }


  
  public String getName() {
    return name;
  }
  
  @Override
  public boolean isStatic() {
    return lockImpl.isStatic();
  }
  
  @Override
  public boolean isIntrinsic(IBinder binder) {
    return lockImpl.isIntrinsic(null);
  }
  
  @Override
  public boolean isJUC(IBinder binder) {
    return lockImpl.isJUC(null);
  }
  
  @Override
  public boolean isReadWrite(IBinder binder) {
    return lockImpl.isReadWrite(null);
  }
}
