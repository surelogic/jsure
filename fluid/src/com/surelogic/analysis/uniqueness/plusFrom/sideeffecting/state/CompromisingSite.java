package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import java.util.Arrays;

import edu.cmu.cs.fluid.ir.IRNode;

public final class CompromisingSite {
  public final IRNode srcOp;
  public final int msg;
  public final Object[] varargs;
  
  public CompromisingSite(
      final IRNode s, final int m, final Object... args) {
    srcOp = s;
    msg = m;
    varargs = args;
  }
  
  @Override
  public boolean equals(final Object o) {
    if (o instanceof CompromisingSite) {
      final CompromisingSite cs = (CompromisingSite) o;
      return 
          msg == cs.msg &&
          srcOp.equals(cs.srcOp) &&
          Arrays.deepEquals(varargs, cs.varargs);
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    int v = 17;
    v += 31 * msg;
    v += 31 * srcOp.hashCode();
    v += 32 * Arrays.hashCode(varargs);
    return v;
  }
}
