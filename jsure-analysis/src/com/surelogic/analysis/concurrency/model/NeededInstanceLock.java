package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;

public final class NeededInstanceLock extends AbstractRealLock {
  private final IRNode objectRefExpr;
  
  public NeededInstanceLock(
      final IRNode objectRefExpr, final LockImplementation lockImpl,
      final IRNode source, final boolean needsWrite) {
    super(source, needsWrite, lockImpl);
    this.objectRefExpr = objectRefExpr;
  }

  @Override
  public int hashCode() {
    int result = 17;
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
      return this.lockImpl.equals(o.lockImpl) && 
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