package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractNeededLock implements NeededLock {
  protected final IRNode source;
  
  protected final boolean needsWrite;
  
  
  
  protected AbstractNeededLock(final IRNode source, final boolean needsWrite) {
    this.source = source;
    this.needsWrite = needsWrite;
  }
  
  @Override
  public final IRNode getSource() {
    return source;
  }
  
  @Override
  public final boolean needsWrite() {
    return needsWrite;
  }
}
