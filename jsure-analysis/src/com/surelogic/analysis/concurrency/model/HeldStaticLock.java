package com.surelogic.analysis.concurrency.model;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

public final class HeldStaticLock extends AbstractHeldLock {
  public HeldStaticLock(
      final LockImplementation lockImpl, final IRNode source,
      final boolean needsWrite, final PromiseDrop<?> supportingDrop) {
    super(source, needsWrite, lockImpl, supportingDrop);
  }
  
  /**
   * Static locks are based on name equivalence.
   */
  @Override
  public boolean mustAlias(final HeldLock lock, final ThisExpressionBinder teb) {
    if (lock instanceof HeldStaticLock) {
      final HeldStaticLock o = (HeldStaticLock) lock;
      return (holdsWrite == o.holdsWrite) && lockImpl.equals(o.lockImpl);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int result = 17;
    result += (holdsWrite ? 1 : 0);
    result += 31 * lockImpl.hashCode();
    result += 31 * source.hashCode();
    result += 31 * ((supportingDrop == null) ? 0 : supportingDrop.hashCode());
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) { 
      return true;
    } else if (other instanceof HeldStaticLock) {
      final HeldStaticLock o = (HeldStaticLock) other;
      return this.holdsWrite == o.holdsWrite &&
          this.lockImpl.equals(o.lockImpl) &&
          this.source.equals(o.source) &&
          (this.supportingDrop == null ? o.supportingDrop == null : this.supportingDrop.equals(o.supportingDrop));
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "<" + lockImpl.getClassName() + 
        lockImpl.getPostfixId() + ">." + 
        (holdsWrite ? "write" : "read") +
        " from " +
        ((supportingDrop == null) ? null : supportingDrop);
  }
}
