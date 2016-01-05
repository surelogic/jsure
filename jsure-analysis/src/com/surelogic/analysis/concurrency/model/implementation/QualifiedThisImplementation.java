package com.surelogic.analysis.concurrency.model.implementation;

import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaSourceRefType;

/**
 * Represents a Class object being used as a lock.
 */
public final class QualifiedThisImplementation implements UnnamedLockImplementation {
  /** The class being used to qualify the receiver */
  private final IJavaSourceRefType clazz;
  
  public QualifiedThisImplementation(final IJavaSourceRefType clazz) {
    this.clazz = clazz;
  }
  
  
  
  @Override
  public String toString() {
    return clazz.toFullyQualifiedText() + ".this";
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
    } else if (other instanceof QualifiedThisImplementation) {
      final QualifiedThisImplementation castOther = (QualifiedThisImplementation) other;
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
  
  // As in SelfImplementation
  @Override
  public String getClassName() {
    return "java.lang.Object";
  }
  
  // As in SelfImplementation
  @Override
  public String getDeclaredInClassName() {
    return "java.lang.Object";
  }
  
  // As in SelfImplementation
  @Override
  public String getPostfixId() {
    return "";
  }
  
  @Override
  public boolean isStatic() { return false; }
  
  @Override
  public boolean isFinalProtected() { return false; }
  
  @Override
  public boolean isIntrinsic(IBinder binder) { return true; }
  
  @Override
  public boolean isJUC(IBinder binder) { return false; }
  
  @Override
  public boolean isReadWrite(IBinder binder) { return false; }
}
