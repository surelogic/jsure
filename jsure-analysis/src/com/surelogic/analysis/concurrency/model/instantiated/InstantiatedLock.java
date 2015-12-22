package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.analysis.concurrency.model.declared.ModelLock;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * An instantiations of a
 * {@link ModelLock}s: in particular, for non-static locks, they are associated
 * with an expression that represents that object whose lock must be held.
 *
 * <p>These are associated with a parse tree node indicating the region access
 * that requires the lock or the expression/declaration that acquires a lock.
 */
public interface InstantiatedLock {
  /**
   * Get the parse tree node of the region access that causes the need for 
   * the lock or that causes the lock to be held.
   */
  public IRNode getSource();
  
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
