package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.aast.IAASTNode;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;

public final class DerivedNeededInstanceLock extends NeededInstanceLock {
  private final NeededInstanceLock derivedFrom;
  
  // Only comes from NeededInstanceLock.replaceEnclosingInstanceReference()
  DerivedNeededInstanceLock(
      final NeededInstanceLock derivedFrom,
      final IRNode objectRefExpr, final LockImplementation lockImpl,
      final IRNode source, final Reason reason,
      final PromiseDrop<? extends IAASTNode> assuredPromise, final boolean needsWrite) {
    super(objectRefExpr, lockImpl, source, reason, assuredPromise, needsWrite);
    this.derivedFrom = derivedFrom;
  }

  @Override
  public NeededLock getDerivedFrom() {
    return derivedFrom;
  }
  
  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * reason.hashCode();
    result += 31 * (needsWrite ? 1 : 0);
    result += 31 * lockImpl.hashCode();
    result += 31 * source.hashCode();
    result += 31 * objectRefExpr.hashCode();
    result += 31 * derivedFrom.hashCode();
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) { 
      return true;
    } else if (other instanceof DerivedNeededInstanceLock) {
      final DerivedNeededInstanceLock o = (DerivedNeededInstanceLock) other;
      return this.reason == o.reason &&
          this.needsWrite == o.needsWrite &&
          this.lockImpl.equals(o.lockImpl) && 
          this.objectRefExpr.equals(o.objectRefExpr) &&
          this.source.equals(o.source) &&
          this.derivedFrom.equals(o.derivedFrom);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "<" + DebugUnparser.toString(objectRefExpr) +
        lockImpl.getPostfixId() + ">." + 
        (needsWrite ? "write" : "read") + 
        " derived from " + derivedFrom.toString();
  }
  
  @Override
  public String unparseForMessage() {
    return "<" + DebugUnparser.toString(objectRefExpr) +
        lockImpl.getPostfixId() + ">." + 
        (needsWrite ? "write" : "read");
  }
}
