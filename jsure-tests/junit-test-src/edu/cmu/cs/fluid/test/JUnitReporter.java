/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/test/JUnitReporter.java,v 1.1 2007/01/23 19:00:19 chance Exp $*/
package edu.cmu.cs.fluid.test;

import junit.framework.Assert;

public class JUnitReporter implements IReporter {
	@Override
  public void reportError(String msg) {
    Assert.fail(msg);
  }
  
  public static final IReporter prototype = new JUnitReporter();
}
