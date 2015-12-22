package com.surelogic.analysis.concurrency.model.declared;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.dropsea.ir.PromiseDrop;

public interface NamedLock<
    A extends PromiseDrop<? extends IAASTRootNode>,
    L extends LockImplementation>
extends ModelLock<A, L> {
  /**
   * The name of this lock.
   */
  public String getName();
}
