/*$Header: /cvs/fluid/fluid/src/com/surelogic/test/ITestOutput.java,v 1.5 2007/11/12 14:25:53 chance Exp $*/
package com.surelogic.test;

/**
 * Standard interface for reporting test events
 *
 * @author Edwin.Chan
 */
public interface ITestOutput {
  void reset();
  ITest reportStart(ITest o);
  void reportSuccess(ITest o, String msg);
  void reportFailure(ITest o, String msg); 
  void reportError(ITest o, Throwable ex);
  Iterable<Object> getUnreported();
  void close();
}
