package com.surelogic.analysis.concurrency.model.instantiated;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractInstantiatedLock implements InstantiatedLock {
  protected final IRNode source;
  

  
  protected AbstractInstantiatedLock(final IRNode source) {
    this.source = source;
  }
  
  @Override
  public final IRNode getSource() {
    return source;
  }
}
