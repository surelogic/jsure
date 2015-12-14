package com.surelogic.analysis.concurrency.model;

import com.surelogic.aast.IAASTNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractNeededLock
extends AbstractInstantiatedLock
implements NeededLock {
  protected final boolean needsWrite;
  
  
  
  protected AbstractNeededLock(
      final IRNode source, final PromiseDrop<? extends IAASTNode> lockPromise,  final boolean needsWrite) {
    super(source, lockPromise);
    this.needsWrite = needsWrite;
  }
  
  @Override
  public final boolean needsWrite() {
    return needsWrite;
  }
}
