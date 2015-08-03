package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * Represents the implementation of a lock.
 *
 */
public interface LockImplementation {
  /**
   * Returns whether the lock is static: whether the field/method representing
   * the lock is static, or if the lock is a class representation.
   */
  public boolean isStatic();
  
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
