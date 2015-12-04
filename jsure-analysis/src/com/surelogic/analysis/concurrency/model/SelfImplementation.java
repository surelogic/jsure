package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * Represents an object instance being used as a lock to protect one of its 
 * own regions.
 */
public final class SelfImplementation implements UnnamedLockImplementation {
  // XXX: Should have a singleton prototype?  See how this class plays out
  public SelfImplementation() {
    super();
  }
  
  
  
  @Override
  public String toString() {
    return "this";
  }
  
  @Override
  public String getClassName() {
    return "java.lang.Object";
  }
  
  @Override
  public String getDeclaredInClassName() {
    return getClassName();
  }
  
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
