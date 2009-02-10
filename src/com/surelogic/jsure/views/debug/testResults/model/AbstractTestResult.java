package com.surelogic.jsure.views.debug.testResults.model;

import com.surelogic.test.ITest;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractTestResult {
  protected final ITest test;
  protected final boolean successful;
  protected final String message;
  
  protected AbstractTestResult(final ITest t, final boolean s, final String msg) {
    test = t;
    successful = s;
    message = msg;
  }
  
  
  
  public final IRNode getNode() {
    return test.getNode();
  }
  
  public final String getMessage() {
    return message;
  }
  
  public final boolean isSuccessful() {
    return successful;
  }
}
