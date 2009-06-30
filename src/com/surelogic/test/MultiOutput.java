/*$Header: /cvs/fluid/fluid/src/com/surelogic/test/MultiOutput.java,v 1.5 2007/11/12 14:25:53 chance Exp $*/
package com.surelogic.test;

public class MultiOutput implements ITestOutput {
  private final ITestOutput o1, o2;

  private MultiOutput(ITestOutput o1, ITestOutput o2) {
    this.o1 = o1;
    this.o2 = o2;
  }

  public ITest reportStart(ITest o) {
    o1.reportStart(o);
    o2.reportStart(o);
    return o;
  }
  
  public void reportError(ITest o, Throwable ex) {
    o1.reportError(o, ex);
    o2.reportError(o, ex);
  } 

  public void reportFailure(ITest o, String msg) {
    o1.reportFailure(o, msg);
    o2.reportFailure(o, msg);
  }

  public void reportSuccess(ITest o, String msg) {
    o1.reportSuccess(o, msg);
    o2.reportSuccess(o, msg);
  }

  public Iterable<Object> getUnreported() {
    return o1.getUnreported();
  }
  
  public void close() {
    o1.close();
    o2.close();
  }
  
  public static ITestOutputFactory makeFactory(final ITestOutputFactory f1, 
                                               final ITestOutputFactory f2) {
    return new ITestOutputFactory() {
      public ITestOutput create(String name) {
        return new MultiOutput(f1.create(name), f2.create(name));
      }
    };
  }
}
