/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/DoubleOutputPort.java,v 1.6 2006/04/14 19:32:32 boyland Exp $ */
package edu.cmu.cs.fluid.control;

abstract class DoubleOutputPort extends OutputPort
    implements TwoInput, MutableControlNode
{
  protected ControlEdge input1, input2;
  
  @Override
  public ControlEdge getInput1() { return input1; }
  @Override
  public ControlEdge getInput2() { return input2; }
  @Override
  public ControlEdge getInput(boolean secondary) {
    return secondary ? input2 : input1;
  }

  @Override
  public void setInput1(ControlEdge e)
      throws EdgeLinkageError
  {
    if (input1 != null) throw new EdgeLinkageError("Input #1 already set.");
    input1 = e;
  }

  @Override
  public void setInput2(ControlEdge e)
      throws EdgeLinkageError
  {
    if (input2 != null) throw new EdgeLinkageError("Input #2 already set.");
    input2 = e;
  }

  @Override
  public ControlEdgeIterator getInputs() {
    return new PairControlEdgeIterator(input1,input2);
  }

  @Override
  public void resetOutput(ControlEdge e) {
	  ((MutableControlNode)getDual()).resetOutput(e);
  }
  
  @Override
  public void resetInput(ControlEdge e) {
	  if (input1 == e) {
		  input1 = null;
	  } else if (input2 == e) {
		  input2 = e;
	  } else {
		  throw new EdgeLinkageError("Not an incoming edge: " + e);
	  }
  }
}
