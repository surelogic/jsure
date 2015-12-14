package com.surelogic.analysis.concurrency.model;

import com.surelogic.aast.IAASTNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractInstantiatedLock implements InstantiatedLock {
  protected final IRNode source;
  protected final PromiseDrop<? extends IAASTNode> lockPromise;
  

  
  protected AbstractInstantiatedLock(
      final IRNode source, final PromiseDrop<? extends IAASTNode> lockPromise) {
    this.source = source;
    this.lockPromise = lockPromise;
  }
  
  @Override
  public final IRNode getSource() {
    return source;
  }
  
  @Override
  public final PromiseDrop<? extends IAASTNode> getLockPromise() {
    return lockPromise;
  }
}
