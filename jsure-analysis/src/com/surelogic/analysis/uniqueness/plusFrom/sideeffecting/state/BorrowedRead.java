package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import edu.cmu.cs.fluid.ir.IRNode;

final class BorrowedRead {
  public final IRNode srcOp;
  public final boolean isAbrupt;
  
  private final int hashCode;
  
  public BorrowedRead(final IRNode n, final boolean a) {
    this.srcOp = n;
    this.isAbrupt = a;
    
    int hc = 17;
    hc = 31 * hc + n.hashCode();
    hc = 31 * hc + (a ? 1 : 0);
    hashCode = hc;
  }
  
  @Override
  public int hashCode() { return hashCode; }
  
  @Override
  public boolean equals(final Object o) {
    if (o instanceof BorrowedRead) {
      final BorrowedRead other = (BorrowedRead) o;
      return srcOp.equals(other.srcOp) && isAbrupt == other.isAbrupt;
    }
    return false;
  }
}
