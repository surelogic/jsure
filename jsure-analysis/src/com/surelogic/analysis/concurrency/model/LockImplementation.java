package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * Represents the implementation of a lock.
 */
public interface LockImplementation {
  /**
   * Get the name of the class whose member or reference is being
   * used as the lock.
   */
  public String getClassName();
  
  /**
   * Get the name of the class where the lock implementation is declared.  For
   * unnamed locks this is the same as {@link #getClassName}.
   */
  public String getDeclaredInClassName();
  
  /**
   * Get the "id" for the lock in a form suitable for appending to a qualifying
   * label.  That is, the id should start with the appropriate separator 
   * notation.
   */
  public String getPostfixId();
  
  /**
   * Returns whether the lock is static: whether the field/method representing
   * the lock is static, or if the lock is a class representation.
   */
  public boolean isStatic();
  
  /**
   * Whether the lock protects final fields or not.  Right now the only time
   * a final field is protected is to implement the "itself" case of the
   * GuardedBy annotation.
   */
  public boolean isFinalProtected();
  
  /** 
   * Returns whether the lock is an intrinsic Java lock, that is one that
   * is acquired using <code>synchronized</code> blocks.  This is mutually 
   * exclusive with {@Link #isJUC}: only one is <code>true</code>.
   */
  public boolean isIntrinsic(IBinder binder);
  
  /**
   * Returns whether the lock is a <code>java.util.concurrent</code> lock, that
   * is one that implements {@link java.util.concurrent.Lock} or
   * {@link java.util.concurrent.ReadWriteLock}. This is mutually exclusive with
   * {@Link #isJUC}: only one is <code>true</code>.
   */
  public boolean isJUC(IBinder binder);
  
  /**
   * Returns whether the lock is a <code>java.util.concurrent</code>
   * read&ndash;write lock, that is one that implements
   * {@link java.util.concurrent.ReadWriteLock}. 
   */
  public boolean isReadWrite(IBinder binder);
  
  // Not sure what else goes here yet
}
