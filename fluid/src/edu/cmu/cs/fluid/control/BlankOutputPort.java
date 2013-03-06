/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/BlankOutputPort.java,v 1.5 2005/05/20 15:48:04 chance Exp $ */
package edu.cmu.cs.fluid.control;

public abstract class BlankOutputPort extends OutputPort implements NoInput {
  @Override
  public ControlEdgeIterator getInputs() {
    return EmptyControlEdgeIterator.prototype;
  }
}
