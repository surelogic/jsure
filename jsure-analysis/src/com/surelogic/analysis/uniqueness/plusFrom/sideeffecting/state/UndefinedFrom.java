package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import edu.cmu.cs.fluid.ir.IRNode;

final class UndefinedFrom {
  public final int message;
  public final IRNode srcOp;
  
  private final int hashCode;
  
  public UndefinedFrom(final int msg, final IRNode n) {
    this.message = msg;
    this.srcOp = n;
    
    int hc = 17;
    hc = 31 * hc + message;
    hc = 31 * hc + srcOp.hashCode();
    hashCode = hc;
  }
  
  @Override
  public int hashCode() { return hashCode; }
  
  @Override
  public boolean equals(final Object o) {
    if (o instanceof UndefinedFrom) {
      final UndefinedFrom other = (UndefinedFrom) o;
      return message == other.message &&
          srcOp.equals(other.srcOp);
    }
    return false;
  }
}
