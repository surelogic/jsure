package com.surelogic.dropsea.ir.drops.uniqueness;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.dropsea.ir.PromiseDrop;


public interface IUniquePromise {
  public PromiseDrop<? extends IAASTRootNode> getDrop();
}
