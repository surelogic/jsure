package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractNeededLock implements NeededLock {
  protected final IRNode source;
  
  protected AbstractNeededLock(final IRNode source) {
    this.source = source;
  }
  
  @Override
  public final IRNode getSource() {
    return source;
  }
}
