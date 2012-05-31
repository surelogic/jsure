package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import edu.cmu.cs.fluid.ir.IRNode;

final class BuriedRead {
  public final Object var;
  public final IRNode srcOp;
  public final boolean isAbrupt;
  
  public BuriedRead(final Object var, final IRNode n, final boolean a) {
    this.var = var;
    this.srcOp = n;
    this.isAbrupt = a;
  }
}
