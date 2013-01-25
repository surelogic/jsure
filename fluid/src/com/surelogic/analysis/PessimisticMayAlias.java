package com.surelogic.analysis;

import com.surelogic.analysis.alias.IMayAlias;

import edu.cmu.cs.fluid.ir.IRNode;

public final class PessimisticMayAlias implements IMayAlias {
  public static final PessimisticMayAlias INSTANCE = new PessimisticMayAlias();
  
  private PessimisticMayAlias() {
    super();
  }
  
  @Override
  public boolean mayAlias(final IRNode expr1, final IRNode expr2) {
    return true;
  }
}
