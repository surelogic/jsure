/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ide/IMemoryPolicy.java,v 1.2 2008/06/26 19:48:42 chance Exp $*/
package edu.cmu.cs.fluid.ide;

public interface IMemoryPolicy {
  boolean addLowMemoryHandler(ILowMemoryHandler h);
  void checkIfLowOnMemory();
  long memoryUsed();
  long memoryLimit();
  
  /**
   * @return the percentage of files to unload
   */
  double percentToUnload();

  void shutdown();
}
