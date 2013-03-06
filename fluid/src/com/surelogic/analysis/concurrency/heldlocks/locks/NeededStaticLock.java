package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.analysis.MethodCallUtils.EnclosingRefs;
import com.surelogic.dropsea.ir.drops.locks.LockModel;


/**
 * Representation of a lock that is represented by a static field.  Such locks
 * only have one instance at runtime.
 */
final class NeededStaticLock extends AbstractNeededLock {
  NeededStaticLock(final LockModel lm, final Type type) {
    super(lm, type);
  }
  
  @Override
  public boolean equals( final Object o ) {
    if( o instanceof NeededStaticLock ) {
      return baseEquals((NeededStaticLock) o);
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

  @Override
  public boolean mayHaveAliasInCallingContext() {
    return false;
  }
  
  @Override
  public NeededLock getAliasInCallingContext(
      final EnclosingRefs enclosingRefs, final NeededLockFactory lockFactory) {
    // Static locks never have an alternative
    return null;
  }
}
