package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.RequiresLock;
import com.surelogic.aast.IAASTNode;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.analysis.concurrency.model.implementation.NamedLockImplementation;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Builder;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

public abstract class AbstractNeededLock
extends AbstractInstantiatedLock
implements NeededLock {
  protected final PromiseDrop<? extends IAASTNode> assuredPromise;
  protected final Reason reason;
  protected final boolean needsWrite;
  protected final LockImplementation lockImpl;
  
  
  
  protected AbstractNeededLock(
      final IRNode source, final Reason reason,
      final PromiseDrop<? extends IAASTNode> assuredPromise,
      final boolean needsWrite, final LockImplementation lockImpl) {
    super(source);
    this.assuredPromise = assuredPromise;
    this.reason = reason;
    this.needsWrite = needsWrite;
    this.lockImpl = lockImpl;
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
  
  @Override
  public final PromiseDrop<? extends IAASTNode> getAssuredPromise() {
    return assuredPromise;
  }
  
  @Override
  public final Reason getReason() {
    return reason;
  }
  
  @Override
  public final boolean needsWrite() {
    return needsWrite;
  }
  
  @Override
  public ProposedPromiseDrop getProposedRequiresLock(
      final IRNode mdecl, final IRNode src) {
    if (lockImpl instanceof NamedLockImplementation) {
      final Builder b = new Builder(RequiresLock.class, mdecl, src);
      b.setValue(((NamedLockImplementation) lockImpl).getName());
      return b.build();
    } else {
      return null;
    }
  }
}
