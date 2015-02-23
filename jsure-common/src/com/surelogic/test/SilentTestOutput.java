/*$Header: /cvs/fluid/fluid/src/com/surelogic/test/SilentTestOutput.java,v 1.1 2008/05/09 18:28:30 aarong Exp $*/
package com.surelogic.test;

/**
 * An implementation of {@link ITestOutput} that does all the tracking of
 * test results, but that doesn't output the results anywhere.
 *
 * @author aarong
 */
public final class SilentTestOutput extends AbstractTestOutput {
  private SilentTestOutput(final String name) {
    super(name);
  }



  @Override
  public void reportError(final ITest o, final Throwable ex) {
    report(o, ex);
  }

  @Override
  public void reportFailure(final ITest o, final String msg) {
    report(o, msg);
  }

  @Override
  public void reportSuccess(final ITest o, final String msg) {
    report(o, msg);
  }



  public static final ITestOutputFactory factory = new ITestOutputFactory() {
    @Override
    public ITestOutput create(String name) {
      return new SilentTestOutput(name);
    }
  };
}
