/*$Header: /cvs/fluid/fluid/src/com/surelogic/test/MultiOutput.java,v 1.5 2007/11/12 14:25:53 chance Exp $*/
package com.surelogic.test;

public class MultiOutput implements ITestOutput {
  private final ITestOutput o1, o2;

  private MultiOutput(ITestOutput o1, ITestOutput o2) {
    this.o1 = o1;
    this.o2 = o2;
  }

  @Override
  public void reset() {
	  o1.reset();
	  o2.reset();
  }
  
  @Override
  public ITest reportStart(ITest o) {
    o1.reportStart(o);
    o2.reportStart(o);
    return o;
  }
  
  @Override
  public void reportError(ITest o, Throwable ex) {
    o1.reportError(o, ex);
    o2.reportError(o, ex);
  } 

  @Override
  public void reportFailure(ITest o, String msg) {
    o1.reportFailure(o, msg);
    o2.reportFailure(o, msg);
  }

  @Override
  public void reportSuccess(ITest o, String msg) {
    o1.reportSuccess(o, msg);
    o2.reportSuccess(o, msg);
  }

  @Override
  public Iterable<Object> getUnreported() {
    return o1.getUnreported();
  }
  
  @Override
  public void close() {
    o1.close();
    o2.close();
  }
  
  @Override
  public String toString() {
	  return o1+", "+o2;
  }
  
  public static ITestOutputFactory makeFactory(final ITestOutputFactory f1, 
                                               final ITestOutputFactory f2) {
    return new ITestOutputFactory() {
      @Override
      public ITestOutput create(String name) {
        return new MultiOutput(f1.create(name), f2.create(name));
      }
    };
  }
}
