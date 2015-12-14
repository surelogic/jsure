package com.surelogic.analysis.concurrency.model;

import com.surelogic.aast.IAASTNode;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.RequiresLockPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

public final class HeldStaticLock extends AbstractHeldLock {
  public HeldStaticLock(
      final LockImplementation lockImpl, final IRNode source,
      final PromiseDrop<? extends IAASTNode> lockPromise,
      final boolean needsWrite, final RequiresLockPromiseDrop supportingDrop) {
    super(source, lockPromise, needsWrite, lockImpl, supportingDrop);
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
  public boolean mustSatisfy(final NeededLock lock, final ThisExpressionBinder teb) {
    if (lock instanceof NeededStaticLock) {
      final NeededStaticLock otherLock = (NeededStaticLock) lock;
      return otherLock.lockImpl.equals(this.lockImpl) &&
          (holdsWrite() || (!holdsWrite() && !otherLock.needsWrite()));
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
    return "<" + lockImpl.getDeclaredInClassName() + 
        lockImpl.getPostfixId() + ">." + 
        (holdsWrite ? "write" : "read");
  }
}
