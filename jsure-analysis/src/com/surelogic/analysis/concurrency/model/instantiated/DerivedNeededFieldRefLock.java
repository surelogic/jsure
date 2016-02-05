package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.aast.IAASTNode;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;

public final class DerivedNeededFieldRefLock extends NeededFieldRefLock {
  private final NeededFieldRefLock derivedFrom;
  
  // Only comes from NeededInstanceLock.replaceEnclosingInstanceReference()
  DerivedNeededFieldRefLock(
      final NeededFieldRefLock derivedFrom,
      final IRNode objectRefExpr, final IRNode varDecl, final LockImplementation lockImpl,
      final IRNode source, final Reason reason,
      final PromiseDrop<? extends IAASTNode> assuredPromise, final boolean needsWrite) {
    super(objectRefExpr, varDecl, lockImpl, source, reason, assuredPromise, needsWrite);
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
    result += 31 * varDecl.hashCode();
    result += 31 * derivedFrom.hashCode();
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) { 
      return true;
    } else if (other instanceof DerivedNeededFieldRefLock) {
      final DerivedNeededFieldRefLock o = (DerivedNeededFieldRefLock) other;
      return this.reason == o.reason &&
          this.needsWrite == o.needsWrite &&
          this.lockImpl.equals(o.lockImpl) && 
          this.objectRefExpr.equals(o.objectRefExpr) &&
          this.varDecl.equals(o.varDecl)&& 
          this.source.equals(o.source) &&
          this.derivedFrom.equals(o.derivedFrom);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "<" + DebugUnparser.toString(objectRefExpr) +
        "." + VariableDeclarator.getId(varDecl) +
        lockImpl.getPostfixId() + ">." + 
        (needsWrite ? "write" : "read") + 
        " derived from " + derivedFrom.toString();
  }
  
  @Override
  public String unparseForMessage() {
    return "<" + DebugUnparser.toString(objectRefExpr) +
        "." + VariableDeclarator.getId(varDecl) +
        lockImpl.getPostfixId() + ">." + 
        (needsWrite ? "write" : "read");
  }
}
