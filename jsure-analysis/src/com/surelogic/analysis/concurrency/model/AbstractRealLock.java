package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

public abstract class AbstractRealLock extends AbstractNeededLock {
  protected final LockImplementation lockImpl;
  
  protected AbstractRealLock(
      final IRNode source, final boolean needsWrite, 
      final LockImplementation lockImpl) {
    super(source, needsWrite);
    this.lockImpl = lockImpl;
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
