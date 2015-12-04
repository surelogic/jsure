package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * A lock implementation associated with a name.  These always come
 * from PolicyLock and RegionLock.
 */
public final class NamedLockImplementation implements LockImplementation {
  /**
   * The class that declares this named lock.
   */
  private final IRNode declaredOn;
  
  /** The name of the lock. */
  private final String name;
  
  /** Delegate to the actual lock implementation. */
  private final UnnamedLockImplementation lockImpl;
  
  
  
  public NamedLockImplementation(final IRNode declaredOn,
      final String name, final UnnamedLockImplementation lockImpl) {
    this.declaredOn = declaredOn;
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
    result += 31 * result + declaredOn.hashCode(); // shouldn't be null
    result += 31 * result + name.hashCode();
    result += 31 * result + lockImpl.hashCode();
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof NamedLockImplementation) {
      final NamedLockImplementation castOther = (NamedLockImplementation) other;
      return this.declaredOn.equals(castOther.declaredOn) &&
          this.name.equals(castOther.name) && 
          this.lockImpl.equals(castOther.lockImpl);
    } else {
      return false;
    }
  }


  
  public String getName() {
    return name;
  }
  
  @Override
  public String getClassName() {
    return lockImpl.getClassName();
  }
  
  @Override
  public String getDeclaredInClassName() {
    return JavaNames.getFullTypeName(declaredOn);
  }
  
  @Override
  public String getPostfixId() {
    return ":" + name;
  }

  @Override
  public boolean isStatic() {
    return lockImpl.isStatic();
  }
  
  @Override
  public boolean isFinalProtected() { return lockImpl.isFinalProtected(); }
  
  @Override
  public boolean isIntrinsic(IBinder binder) {
    return lockImpl.isIntrinsic(binder);
  }
  
  @Override
  public boolean isJUC(IBinder binder) {
    return lockImpl.isJUC(binder);
  }
  
  @Override
  public boolean isReadWrite(IBinder binder) {
    return lockImpl.isReadWrite(binder);
  }
}
