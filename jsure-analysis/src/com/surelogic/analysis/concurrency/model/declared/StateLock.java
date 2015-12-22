package com.surelogic.analysis.concurrency.model.declared;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
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
  
  /**
   * Does the lock protect the given region.  Takes into account the region
   * hierarchy, as well as any modifiers (final, volatile, etc.) on the 
   * region.
   */
  public boolean protects(IRegion region);
}
