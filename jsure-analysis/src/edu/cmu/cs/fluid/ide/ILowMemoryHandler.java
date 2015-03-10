/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ide/ILowMemoryHandler.java,v 1.1 2007/04/13 20:51:02 chance Exp $*/
package edu.cmu.cs.fluid.ide;

public interface ILowMemoryHandler {
  void handleLowMemory(IMemoryPolicy mp);
}
