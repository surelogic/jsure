package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;

public final class NeededStaticLock extends AbstractRealLock {
  public NeededStaticLock(
      final LockImplementation lockImpl, final IRNode source,
      final boolean needsWrite) {
    super(source, needsWrite, lockImpl);
  }

  @Override
  public int hashCode() {
    int result = 17;
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
      return this.lockImpl.equals(o.lockImpl) &&
          this.source.equals(o.source);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "<" + lockImpl.getClassName() + 
        lockImpl.getPostfixId() + ">." + 
        (needsWrite ? "write" : "read");
  }
}
