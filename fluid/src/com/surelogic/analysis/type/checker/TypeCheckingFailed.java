package com.surelogic.analysis.type.checker;

import edu.cmu.cs.fluid.ir.IRNode;

public class TypeCheckingFailed extends Exception {
  private final IRNode expr;
  private final ITypeError error;
  public TypeCheckingFailed(final IRNode e, final ITypeError err) {
    super(err.getMessage());
    expr = e;
    error = err;
  }
  
  public IRNode getExpression() {
    return expr;
  }
  
  public ITypeError getError() {
    return error;
  }
}
