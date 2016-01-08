package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.concurrency.model.SyntacticEquality;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.RequiresLockPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;

public final class HeldInstanceLock extends AbstractHeldLock {
  private final IRNode objectRefExpr;
  
  // Must use the HeldLockFactory
  HeldInstanceLock(
      final IRNode objectRefExpr, final LockImplementation lockImpl,
      final IRNode source, final Reason reason, final boolean needsWrite,
      final PromiseDrop<?> lockPromise,
      final RequiresLockPromiseDrop supportingDrop) {
    super(source, reason, needsWrite, lockImpl, lockPromise, supportingDrop);
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
  public boolean mustSatisfy(final NeededLock lock, final ThisExpressionBinder teb) {
    if (lock instanceof NeededInstanceLock) {
      final NeededInstanceLock o = (NeededInstanceLock) lock;
      return (holdsWrite || (!holdsWrite && !o.needsWrite())) &&
          lockImpl.equals(o.lockImpl) &&
          SyntacticEquality.checkSyntacticEquality(objectRefExpr, o.objectRefExpr, teb);
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * reason.hashCode();
    result += 31 * (holdsWrite ? 1 : 0);
    result += 31 * lockImpl.hashCode();
    result += 31 * source.hashCode();
    result += 31 * objectRefExpr.hashCode();
    result += 31 * ((lockPromise == null) ? 0 : lockPromise.hashCode());
    result += 31 * ((supportingDrop == null) ? 0 : supportingDrop.hashCode());
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) { 
      return true;
    } else if (other instanceof HeldInstanceLock) {
      final HeldInstanceLock o = (HeldInstanceLock) other;
      return this.reason == o.reason &&
          this.holdsWrite == o.holdsWrite &&
          this.lockImpl.equals(o.lockImpl) && 
          this.objectRefExpr.equals(o.objectRefExpr) &&
          this.source.equals(o.source) &&
          (this.lockPromise == null ? o.lockPromise == null : this.lockPromise.equals(o.lockPromise)) && 
          (this.supportingDrop == null ? o.supportingDrop == null : this.supportingDrop.equals(o.supportingDrop));
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "<" + DebugUnparser.toString(objectRefExpr) +
        lockImpl.getPostfixId() + ">." + 
        (holdsWrite ? "write" : "read");
  }
}
