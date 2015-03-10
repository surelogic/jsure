/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ide/DefaultMemoryPolicy.java,v 1.2 2008/06/26 19:48:42 chance Exp $*/
package edu.cmu.cs.fluid.ide;

/**
 * A default policy that does nothing.
 * 
 * @author Edwin.Chan
 */
public final class DefaultMemoryPolicy extends AbstractMemoryPolicy {
  public static final IMemoryPolicy prototype = new DefaultMemoryPolicy();

  @Override
  public void checkIfLowOnMemory() {
	  // Nothing to do
  }
  
  @Override
  public double percentToUnload() {
    return 0;
  }
}
