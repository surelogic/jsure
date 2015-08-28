package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractNeededLock implements NeededLock {
  protected final ModelLock<?, ?> modelLock;
  protected final IRNode source;
  
  protected AbstractNeededLock(final ModelLock<?, ?> modelLock, final IRNode source) {
    this.modelLock = modelLock;
    this.source = source;
  }
  
  @Override
  public final IRNode getSource() {
    return source;
  }
}
