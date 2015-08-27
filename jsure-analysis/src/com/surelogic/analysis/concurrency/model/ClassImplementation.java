package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaSourceRefType;

/**
 * Represents a Class object being used as a lock.
 */
public final class ClassImplementation implements UnnamedLockImplementation {
  /** The class being used as the lock. */
  private final IJavaSourceRefType clazz;
  
  public ClassImplementation(final IJavaSourceRefType clazz) {
    this.clazz = clazz;
  }
  
  
  
  @Override
  public String toString() {
    return clazz.toFullyQualifiedText() + ".class";
  }
  
  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * clazz.hashCode();
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof ClassImplementation) {
      final ClassImplementation castOther = (ClassImplementation) other;
      return this.clazz.equals(castOther.clazz);
    } else {
      return false;
    }
  }
  
  
  
  /**
   * Get the class used a lock.  (Note: {@link #getClass()} is already taken.)
   */
  public IJavaSourceRefType getClazz() {
    return clazz;
  }
  
  @Override
  public String getClassName() {
    return clazz.toFullyQualifiedText();
  }
  
  @Override
  public String getPostfixId() {
    return ".class";
  }
  
  @Override
  public boolean isStatic() { return true; }
  
  @Override
  public boolean isFinalProtected() { return false; }
  
  @Override
  public boolean isIntrinsic(IBinder binder) { return true; }
  
  @Override
  public boolean isJUC(IBinder binder) { return false; }
  
  @Override
  public boolean isReadWrite(IBinder binder) { return false; }
}
