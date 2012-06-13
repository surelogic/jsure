package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import edu.cmu.cs.fluid.ir.IRNode;

final class BuriedRead {
  public final BuriedMessage message;
  public final Object var;
  public final IRNode srcOp;
  public final boolean isAbrupt;
  
  public BuriedRead(final BuriedMessage msg,
      final Object var, final IRNode n, final boolean a) {
    this.message = msg;
    this.var = var;
    this.srcOp = n;
    this.isAbrupt = a;
  }
  
  public int getMessage() { return message.getMessage(); }
  
  public Object[] getVarArgs() { return message.getVarArgs(var); }
}
