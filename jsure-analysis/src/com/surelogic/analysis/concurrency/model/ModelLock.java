package com.surelogic.analysis.concurrency.model;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;

/**
 * Lock in the lock model.
 */
public interface ModelLock<A extends PromiseDrop<? extends IAASTRootNode>, L extends LockImplementation> {
  /**
   * Get the annotation that generated this lock.
   * Could be a RegionLock, PolicyLock, or GuardedBy annotation.
   */
  public A getSourceAnnotation();
  
  /**
   * Get the lock implementation.
   */
  public L getImplementation();

  /**
   * Get the class in which the lock is declared.
   */
  public IJavaDeclaredType getDeclaredInClass();
  
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
}
