package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.aast.IAASTNode;
import com.surelogic.analysis.MethodCallUtils.EnclosingRefs;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;

public final class NeededInstanceLock extends AbstractNeededLock {
  // needs to be visible to HeldInstanceLock
  final IRNode objectRefExpr;
  
  // Must use NeededLockFactory
  NeededInstanceLock(
      final IRNode objectRefExpr, final LockImplementation lockImpl,
      final IRNode source, final Reason reason,
      final PromiseDrop<? extends IAASTNode> assuredPromise, final boolean needsWrite) {
    super(source, reason, assuredPromise, needsWrite, lockImpl);
    this.objectRefExpr = objectRefExpr;
  }

  @Override
  public NeededLock replaceEnclosingInstanceReference(final EnclosingRefs refs) {
    final IRNode newRef = refs.replace(objectRefExpr);
    if (newRef == null) { // no change
      return this;
    } else {
      return new NeededInstanceLock(newRef, lockImpl, source, reason, assuredPromise, needsWrite);
    }
  }

  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * reason.hashCode();
    result += 31 * (needsWrite ? 1 : 0);
    result += 31 * lockImpl.hashCode();
    result += 31 * source.hashCode();
    result += 31 * objectRefExpr.hashCode();
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) { 
      return true;
    } else if (other instanceof NeededInstanceLock) {
      final NeededInstanceLock o = (NeededInstanceLock) other;
      return this.reason == o.reason &&
          this.needsWrite == o.needsWrite &&
          this.lockImpl.equals(o.lockImpl) && 
          this.objectRefExpr.equals(o.objectRefExpr) &&
          this.source.equals(o.source);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "<" + DebugUnparser.toString(objectRefExpr) +
        lockImpl.getPostfixId() + ">." + 
        (needsWrite ? "write" : "read");
  }
}
