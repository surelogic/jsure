package com.surelogic.analysis.concurrency.model;

import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractHeldLock
extends AbstractInstantiatedLock
implements HeldLock {
  protected final boolean holdsWrite;
  
  protected final LockImplementation lockImpl;

  /**
   * Promise drop for any supporting annotations. Links the correctness of
   * holding this lock with the assurance of the given annotation. If not
   * applicable to this lock, e.g., the lock is from a returns lock annotation,
   * it must be <code>null</code>.
   */
  protected final PromiseDrop<?> supportingDrop;
  

  
  protected AbstractHeldLock(
      final IRNode source, final boolean holdsWrite, 
      final LockImplementation lockImpl, final PromiseDrop<?> supportingDrop) {
    super(source);
    this.holdsWrite = holdsWrite;
    this.lockImpl = lockImpl;
    this.supportingDrop = supportingDrop;
  }
  
  @Override
  public final boolean holdsWrite() {
    return holdsWrite;
  }
  
  @Override
  public final PromiseDrop<?> getSupportingDrop() {
    return supportingDrop;
  }
}
