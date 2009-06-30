/*
 * Created on Jan 21, 2004
 */
package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.promise.IPromiseCheckReport;

/**
 * @author aarong
 */
public class NullPromiseCheckReport implements IPromiseCheckReport {
  public static final NullPromiseCheckReport prototype =
    new NullPromiseCheckReport();
  
  private NullPromiseCheckReport()
  {
    // do nothing
    // private as part of the singleton pattern
  }
  
  public void reportWarning(String description, IRNode promise) {
    // do nothing
  }

  public void reportError(String description, IRNode promise) {
    // do nothing
  }
}
