/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/UnversionedJavaCanonicalizer.java,v 1.2 2008/08/25 15:32:18 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;

public class UnversionedJavaCanonicalizer extends JavaCanonicalizer {
  public UnversionedJavaCanonicalizer(IBinder b) {
    super(b);
  }

  @Override
  protected IBinder fixBinder(IBinder b) {
    return b;
  }
  
  @Override
  protected void checkNode(IRNode node) {
	  // Nothing to do here
  }
}
