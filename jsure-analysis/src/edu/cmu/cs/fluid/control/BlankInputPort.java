/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/BlankInputPort.java,v 1.5 2005/05/20 15:48:03 chance Exp $ */
package edu.cmu.cs.fluid.control;

abstract class BlankInputPort extends InputPort implements NoOutput {
  @Override
  public ControlEdgeIterator getOutputs() {
    return EmptyControlEdgeIterator.prototype;
  }
}
