/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/DoubleOutputPort.java,v 1.6 2006/04/14 19:32:32 boyland Exp $ */
package edu.cmu.cs.fluid.control;

public abstract class DoubleOutputPort extends OutputPort
    implements TwoInput
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
}
