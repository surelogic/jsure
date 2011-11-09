package com.surelogic.jsure.views.debug.testResults.model;

import com.surelogic.test.ITest;

public final class SuccessfulTestResult extends AbstractTestResult {
  public SuccessfulTestResult(final ITest test, final String message) {
    super(test, true, message);
  }
}
