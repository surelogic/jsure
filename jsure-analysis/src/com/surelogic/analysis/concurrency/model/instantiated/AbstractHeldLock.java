package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.RequiresLockPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

public abstract class AbstractHeldLock
extends AbstractInstantiatedLock
implements HeldLock {
  protected final Reason reason;
  protected final boolean holdsWrite;
  
  protected final LockImplementation lockImpl;
  protected final PromiseDrop<?> lockPromise;

  /**
   * Promise drop for any supporting annotations. Links the correctness of
   * holding this lock with the assurance of the given annotation. If not
   * applicable to this lock, e.g., the lock is from a returns lock annotation,
   * it must be <code>null</code>.
   */
  protected final RequiresLockPromiseDrop supportingDrop;
  

  
  protected AbstractHeldLock(
      final IRNode source, final Reason reason,
      final boolean holdsWrite, final LockImplementation lockImpl,
      final PromiseDrop<?> lockPromise,
      final RequiresLockPromiseDrop supportingDrop) {
    super(source);
    this.reason = reason;
    this.holdsWrite = holdsWrite;
    this.lockImpl = lockImpl;
    this.lockPromise = lockPromise;
    this.supportingDrop = supportingDrop;
  }
  
  @Override
  public final Reason getReason() {
    return reason;
  }
  
  @Override
  public final boolean holdsWrite() {
    return holdsWrite;
  }
  
  @Override
  public final PromiseDrop<?> getLockPromise() {
    return lockPromise;
  }
  
  @Override
  public final RequiresLockPromiseDrop getSupportingPromise() {
    return supportingDrop;
  }
  
  @Override
  public final boolean isStatic() {
    return lockImpl.isStatic();
  }
  
  @Override
  public final boolean isIntrinsic(final IBinder binder) {
    return lockImpl.isIntrinsic(binder);
  }
  
  @Override
  public final boolean isJUC(final IBinder binder) {
    return lockImpl.isJUC(binder);
  }
  
  @Override
  public final boolean isReadWrite(final IBinder binder) {
    return lockImpl.isReadWrite(binder);
  }
}
