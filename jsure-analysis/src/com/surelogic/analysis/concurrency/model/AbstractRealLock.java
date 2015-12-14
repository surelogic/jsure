package com.surelogic.analysis.concurrency.model;

import com.surelogic.aast.IAASTNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

public abstract class AbstractRealLock extends AbstractNeededLock {
  protected final LockImplementation lockImpl;
  
  protected AbstractRealLock(
      final IRNode source, final PromiseDrop<? extends IAASTNode> lockPromise, final boolean needsWrite, 
      final LockImplementation lockImpl) {
    super(source, lockPromise, needsWrite);
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
