/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/Split.java,v 1.11 2006/04/14 19:32:32 boyland Exp $ */
package edu.cmu.cs.fluid.control;


/** Abstract class for control-flow nodes with one input and two outputs
 * @author John Tang Boyland
 * @see Fork
 * @see Choice
 * @see TrackedDemerge
 * @see LabelTest
 */

public abstract class Split extends Entity
     implements ControlNode, OneInput, TwoOutput
{
  protected ControlEdge input, output1, output2;
  @Override
  public ControlEdge getInput() { return input; }
  @Override
  public ControlEdge getOutput1() { return output1; }
  @Override
  public ControlEdge getOutput2() { return output2; }
  @Override
  public ControlEdge getOutput(boolean secondary) {
    return secondary ? output2 : output1;
  }
  @Override
  public ControlEdgeIterator getInputs() {
    return new SingleControlEdgeIterator(input);
  }
  @Override
  public ControlEdgeIterator getOutputs() {
    return new PairControlEdgeIterator(output1,output2);
  }
  @Override
  public void setInput(ControlEdge input) 
      throws EdgeLinkageError
  {
    if (this.input != null) throw new EdgeLinkageError("input already set");
    this.input = input;
  }
  @Override
  public void setOutput1(ControlEdge output1) 
      throws EdgeLinkageError
  {
    if (this.output1 != null) 
	throw new EdgeLinkageError("output #1 already set");
    this.output1 = output1;
  }
  @Override
  public void setOutput2(ControlEdge output2) 
      throws EdgeLinkageError
  {
    if (this.output2 != null)
	throw new EdgeLinkageError("output #2 already set");
    this.output2 = output2;
  }
}

