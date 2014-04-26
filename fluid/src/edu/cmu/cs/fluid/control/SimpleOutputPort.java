/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/SimpleOutputPort.java,v 1.5 2005/05/20 15:48:03 chance Exp $ */
package edu.cmu.cs.fluid.control;

abstract class SimpleOutputPort extends OutputPort 
    implements OneInput, MutableControlNode
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
  @Override
  public void resetOutput(ControlEdge e) {
	  ((MutableControlNode)getDual()).resetOutput(e);
  }
  
  @Override
  public void resetInput(ControlEdge e) {
	  if (input == e) {
		  input = null;
	  } else {
		  throw new EdgeLinkageError("Not an incoming edge: " + e);
	  }
  }
}
