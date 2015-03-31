package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import edu.cmu.cs.fluid.ir.IRNode;

final class BuriedRead {
  public final BuriedMessage message;
  public final Object var;
  public final IRNode srcOp;
  public final boolean isAbrupt;
  
  private final int hashCode;
  
  public BuriedRead(final BuriedMessage msg,
      final Object var, final IRNode n, final boolean a) {
    this.message = msg;
    this.var = var;
    this.srcOp = n;
    this.isAbrupt = a;
    
    int hc = 17;
    hc = 31 * hc + msg.hashCode();
    hc = 31 * hc + var.hashCode();
    hc = 31 * hc + n.hashCode();
    hc = 31 * hc + (a ? 1 : 0);
    hashCode = hc;
  }
  
  @Override
  public int hashCode() { return hashCode; }
  
  @Override
  public boolean equals(final Object o) {
    if (o instanceof BuriedRead) {
      final BuriedRead other = (BuriedRead) o;
      return message == other.message &&
          var.equals(other.var) &&
          srcOp.equals(other.srcOp) &&
          isAbrupt == other.isAbrupt;
    }
    return false;
  }
  
  public int getMessage() { return message.getMessage(); }
  
  public Object[] getVarArgs() { return message.getVarArgs(var); }
}