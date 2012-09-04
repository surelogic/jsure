/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/test/IReporter.java,v 1.1 2007/01/23 19:00:19 chance Exp $*/
package edu.cmu.cs.fluid.test;

public interface IReporter {
  void reportError(String msg);

  static final IReporter prototype = new IReporter() {
	  @Override
    public void reportError(String msg) {
      System.out.println(msg);
    }
  };
}
