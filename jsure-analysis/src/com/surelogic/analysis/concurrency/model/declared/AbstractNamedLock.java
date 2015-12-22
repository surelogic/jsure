package com.surelogic.analysis.concurrency.model.declared;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.concurrency.model.implementation.NamedLockImplementation;
import com.surelogic.dropsea.ir.PromiseDrop;

public abstract class AbstractNamedLock<
    A extends PromiseDrop<? extends IAASTRootNode>>
extends AbstractModelLock<A, NamedLockImplementation>
implements NamedLock<A, NamedLockImplementation> {
  public AbstractNamedLock(
      final A sourceDrop, final NamedLockImplementation lockImpl) {
    super(sourceDrop, lockImpl, sourceDrop.getNode());
  }
  
  @Override
  public final String getName() {
    return lockImpl.getName();
  }
}
