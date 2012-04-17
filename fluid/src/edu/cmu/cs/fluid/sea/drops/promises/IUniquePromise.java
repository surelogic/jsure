package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.IAASTRootNode;

import edu.cmu.cs.fluid.sea.PromiseDrop;

public interface IUniquePromise {
  public boolean allowRead();
  public PromiseDrop<? extends IAASTRootNode> getDrop();
}
