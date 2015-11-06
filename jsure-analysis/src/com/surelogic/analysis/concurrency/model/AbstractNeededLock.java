package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractNeededLock
extends AbstractInstantiatedLock
implements NeededLock {
  protected final boolean needsWrite;
  
  
  
  protected AbstractNeededLock(final IRNode source, final boolean needsWrite) {
    super(source);
    this.needsWrite = needsWrite;
  }
  
  @Override
  public final boolean needsWrite() {
    return needsWrite;
  }
}
