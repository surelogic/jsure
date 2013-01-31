/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/DoubleInputPort.java,v 1.6 2006/04/14 19:32:32 boyland Exp $ */
package edu.cmu.cs.fluid.control;

public abstract class DoubleInputPort extends InputPort
    implements TwoOutput
{
  protected ControlEdge output1, output2;
  
  @Override
  public ControlEdge getOutput1() { return output1; }
  @Override
  public ControlEdge getOutput2() { return output2; }
  @Override
  public ControlEdge getOutput(boolean secondary) {
    return secondary ? output2 : output1;
  }

  @Override
  public void setOutput1(ControlEdge e)
      throws EdgeLinkageError
  {
    if (output1 != null) throw new EdgeLinkageError("Output #1 already set.");
    output1 = e;
  }

  @Override
  public void setOutput2(ControlEdge e)
      throws EdgeLinkageError
  {
    if (output2 != null) throw new EdgeLinkageError("Output #2 already set.");
    output2 = e;
  }

  @Override
  public ControlEdgeIterator getOutputs() {
    return new PairControlEdgeIterator(output1,output2);
  }
}
