package com.surelogic.analysis.concurrency.model;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.dropsea.ir.PromiseDrop;

public interface StateLock<
    A extends PromiseDrop<? extends IAASTRootNode>,
    L extends LockImplementation>
extends ModelLock<A, L> {
  /**
   * The region protected by this lock.
   */
  public IRegion getRegion();
}
