package com.surelogic.jsure.views.debug.testResults.model;

import com.surelogic.test.ITest;

public final class ExceptionalTestResult extends AbstractTestResult {
  private final Throwable throwable;
  
  public ExceptionalTestResult(final ITest test, final Throwable t) {
    super(test, false, t.getMessage());
    throwable = t;
  }
  
  public Throwable getThrowable() {
    return throwable;
  }
}
