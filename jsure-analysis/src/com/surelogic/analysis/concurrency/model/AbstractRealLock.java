package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractRealLock extends AbstractNeededLock {
  protected final ModelLock<?, ?> modelLock;
  
  protected AbstractRealLock(final IRNode source, final ModelLock<?, ?> modelLock) {
    super(source);
    this.modelLock = modelLock;
  }
}
