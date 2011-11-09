package com.surelogic.jsure.views.debug.testResults.model;

import com.surelogic.test.ITest;

public final class FailedTestResult extends AbstractTestResult {
  public FailedTestResult(final ITest test, final String message) {
    super(test, false, message);
  }
}
