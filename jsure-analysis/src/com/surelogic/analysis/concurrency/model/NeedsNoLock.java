package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;

public final class NeedsNoLock extends AbstractNeededLock {

  public NeedsNoLock(final IRNode source) {
    super(source);
  }
}
