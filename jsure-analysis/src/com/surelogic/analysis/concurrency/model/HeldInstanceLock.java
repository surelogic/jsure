package com.surelogic.analysis.concurrency.model;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;

public final class HeldInstanceLock extends AbstractHeldLock {
  private final IRNode objectRefExpr;
  
  public HeldInstanceLock(
      final IRNode objectRefExpr, final LockImplementation lockImpl,
      final IRNode source, final boolean needsWrite,
      final PromiseDrop<?> supportingDrop) {
    super(source, needsWrite, lockImpl, supportingDrop);
    this.objectRefExpr = objectRefExpr;
  }

  /**
   * Check that the same lock is used, and then use syntactic equality of the
   * object expressions.
   */
  @Override
  public boolean mustAlias(final HeldLock lock, final ThisExpressionBinder teb) {
    if (lock instanceof HeldInstanceLock) {
      final HeldInstanceLock o = (HeldInstanceLock) lock;
      return (holdsWrite == o.holdsWrite) && lockImpl.equals(o.lockImpl) &&
          SyntacticEquality.checkSyntacticEquality(objectRefExpr, o.objectRefExpr, teb);
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
    result += 31 * objectRefExpr.hashCode();
    result += 31 * ((supportingDrop == null) ? 0 : supportingDrop.hashCode());
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) { 
      return true;
    } else if (other instanceof HeldInstanceLock) {
      final HeldInstanceLock o = (HeldInstanceLock) other;
      return this.holdsWrite == o.holdsWrite &&
          this.lockImpl.equals(o.lockImpl) && 
          this.objectRefExpr.equals(o.objectRefExpr) &&
          this.source.equals(o.source) &&
          (this.supportingDrop == null ? o.supportingDrop == null : this.supportingDrop.equals(o.supportingDrop));
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "<" + DebugUnparser.toString(objectRefExpr) +
        lockImpl.getPostfixId() + ">." + 
        (holdsWrite ? "write" : "read") +
        " from " +
        ((supportingDrop == null) ? null : supportingDrop);
  }
}
