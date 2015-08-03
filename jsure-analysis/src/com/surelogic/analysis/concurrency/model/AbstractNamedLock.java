package com.surelogic.analysis.concurrency.model;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.dropsea.ir.PromiseDrop;

public abstract class AbstractNamedLock<A extends PromiseDrop<? extends IAASTRootNode>> extends AbstractModelLock<A, NamedLockImplementation>{
  public AbstractNamedLock(final A sourceDrop, final NamedLockImplementation lockImpl) {
    super(sourceDrop, lockImpl);
  }
}
