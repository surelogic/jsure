/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/SimpleInputPort.java,v 1.5 2005/05/20 15:48:03 chance Exp $ */
package edu.cmu.cs.fluid.control;

public abstract class SimpleInputPort extends InputPort 
    implements OneOutput
{
  protected ControlEdge output;

  @Override
  public ControlEdge getOutput() { return output; }

  @Override
  public void setOutput(ControlEdge e) 
      throws EdgeLinkageError
  { 
    if (output != null) throw new EdgeLinkageError("Output already set.");
    output = e; 
  }

  @Override
  public ControlEdgeIterator getOutputs() {
    return new SingleControlEdgeIterator(output);
  }
  
}
