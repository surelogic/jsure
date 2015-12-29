package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.aast.IAASTNode;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;

public final class NormalNeededInstanceLock extends NeededInstanceLock {
  // Must use NeededLockFactory
  NormalNeededInstanceLock(
      final IRNode objectRefExpr, final LockImplementation lockImpl,
      final IRNode source, final Reason reason,
      final PromiseDrop<? extends IAASTNode> assuredPromise, final boolean needsWrite) {
    super(objectRefExpr, lockImpl, source, reason, assuredPromise, needsWrite);
  }

  @Override
  public NeededLock getDerivedFrom() {
    return null;
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
    } else if (other instanceof NormalNeededInstanceLock) {
      final NormalNeededInstanceLock o = (NormalNeededInstanceLock) other;
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
  
  @Override
  public String unparseForMessage() {
    return toString();
  }
}
