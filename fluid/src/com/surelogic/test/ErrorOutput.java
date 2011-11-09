/*$Header: /cvs/fluid/fluid/src/com/surelogic/test/ErrorOutput.java,v 1.3 2007/08/01 21:15:18 chance Exp $*/
package com.surelogic.test;

public class ErrorOutput extends AbstractTestOutput {
  private ErrorOutput(String s) {
    super(s);
  }

  public void reportError(ITest o, Throwable ex) {
    throw new Error(ex);
  }

  public void reportFailure(ITest o, String msg) {
    throw new Error(msg);
  }

  public void reportSuccess(ITest o, String msg) {
    report(o, msg);
  }
  
  public static final ITestOutput prototype = new ErrorOutput("Error");
  
  public static final ITestOutputFactory factory = new ITestOutputFactory() {
    public ITestOutput create(String name) {
      return prototype;
    }
  };
}
