package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractRealLock extends AbstractNeededLock {
  protected final LockImplementation lockImpl;
  
  protected AbstractRealLock(
      final IRNode source, final boolean needsWrite, 
      final LockImplementation lockImpl) {
    super(source, needsWrite);
    this.lockImpl = lockImpl;
  }
}
