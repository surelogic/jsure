package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.aast.IAASTNode;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

public final class NeededStaticLock extends AbstractNeededLock {
  // Must use NeededLockFactory
  NeededStaticLock(
      final LockImplementation lockImpl, final IRNode source,
      final Reason reason,
      final PromiseDrop<? extends IAASTNode> lockPromise, final boolean needsWrite) {
    super(source, reason, lockPromise, needsWrite, lockImpl);
  }

  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * reason.hashCode();
    result += 31 * (needsWrite ? 1 : 0);
    result += 31 * lockImpl.hashCode();
    result += 31 * source.hashCode();
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) { 
      return true;
    } else if (other instanceof NeededStaticLock) {
      final NeededStaticLock o = (NeededStaticLock) other;
      return this.reason == o.reason &&
          this.needsWrite == o.needsWrite &&
          this.lockImpl.equals(o.lockImpl) &&
          this.source.equals(o.source);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "<" + lockImpl.getDeclaredInClassName() + 
        lockImpl.getPostfixId() + ">." + 
        (needsWrite ? "write" : "read");
  }
}
