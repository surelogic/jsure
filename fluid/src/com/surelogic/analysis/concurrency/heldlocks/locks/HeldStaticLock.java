package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.analysis.ThisExpressionBinder;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;

/**
 * Representation of a lock that is represented by a static field.  Such locks
 * only have one instance at runtime.
 */
final class HeldStaticLock extends AbstractHeldLock {
  HeldStaticLock(
      final LockModel ld, final IRNode src, final PromiseDrop<?> sd,
      final boolean assumed, final Type type) {
    super(ld, src, sd, assumed, type);
  }
  
  public HeldLock changeSource(final IRNode newSrc) {
    return new HeldStaticLock(lockPromise, newSrc, supportingDrop, isAssumed, type);
  }
  
  /**
   * Static locks are based on name equivalence.
   */
  public boolean mustAlias(final HeldLock lock, final ThisExpressionBinder teb, final IBinder b) {
    if(lock instanceof HeldStaticLock) {
      return (isWrite() == lock.isWrite())
        && getUniqueIdentifier().equals(((HeldStaticLock) lock).getUniqueIdentifier());
    } else {
      return false;
    }
  }
  
  /**
   * One static lock satisfies another if they refer to same lock declaration,
   * and this lock is a writeLock or both locks are read locks.
   */
  public boolean mustSatisfy(final NeededLock lock, final ThisExpressionBinder teb, final IBinder b) {
    if(lock instanceof NeededStaticLock) {
      if (getUniqueIdentifier().equals(((NeededStaticLock) lock).getUniqueIdentifier())) {
        return isWrite() || (!isWrite() && !lock.isWrite());
      } 
    }
    return false;
  }
  
  @Override
  public boolean equals(final Object o) {
    if( o instanceof HeldStaticLock ) {
      return baseEquals((HeldStaticLock) o);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getName());
    sb.append(type.getPostFix());
    return sb.toString();
  }
}
