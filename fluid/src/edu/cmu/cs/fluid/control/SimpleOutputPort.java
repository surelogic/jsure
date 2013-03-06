/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/SimpleOutputPort.java,v 1.5 2005/05/20 15:48:03 chance Exp $ */
package edu.cmu.cs.fluid.control;

public abstract class SimpleOutputPort extends OutputPort 
    implements OneInput
{
  protected ControlEdge input;

  @Override
  public ControlEdge getInput() { return input; }

  @Override
  public void setInput(ControlEdge e) 
      throws EdgeLinkageError
  { 
    if (input != null) throw new EdgeLinkageError("Input already set.");
    input = e; 
  }

  @Override
  public ControlEdgeIterator getInputs() {
    return new SingleControlEdgeIterator(input);
  }
  
}
