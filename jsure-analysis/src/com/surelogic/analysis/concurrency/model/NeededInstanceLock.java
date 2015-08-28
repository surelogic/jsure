package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;

public final class NeededInstanceLock extends AbstractNeededLock {
  private final IRNode objectRefExpr;
  
  public NeededInstanceLock(
      final IRNode objectRefExpr, final ModelLock<?, ?> modelLock,
      final IRNode source) {
    super(modelLock, source);
    this.objectRefExpr = objectRefExpr;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * modelLock.hashCode();
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
      return this.modelLock.equals(o.modelLock) && 
          this.objectRefExpr.equals(o.objectRefExpr) &&
          this.source.equals(o.source);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return DebugUnparser.toString(objectRefExpr) + modelLock.getImplementation().getPostfixId();
  }
}
