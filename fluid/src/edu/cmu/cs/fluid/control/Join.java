/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/Join.java,v 1.11 2006/04/14 19:32:32 boyland Exp $ */
package edu.cmu.cs.fluid.control;


/** Abstract class for control-flow nodes with two inputs and one output
 * @author John Tang Boyland
 * @see Merge
 * @see TrackedMerge
 */

public abstract class Join extends Entity
     implements ControlNode, TwoInput, OneOutput
{
  protected ControlEdge input1, input2, output;
  @Override
  public ControlEdge getInput1() { return input1; }
  @Override
  public ControlEdge getInput2() { return input2; }
  @Override
  public ControlEdge getInput(boolean secondary) { 
    return secondary?input2 : input1;
  }
  @Override
  public ControlEdge getOutput() { return output; }
  @Override
  public ControlEdgeIterator getInputs() {
    return new PairControlEdgeIterator(input1,input2);
  }
  @Override
  public ControlEdgeIterator getOutputs() {
    return new SingleControlEdgeIterator(output);
  }
  @Override
  public void setInput1(ControlEdge input1) 
      throws EdgeLinkageError
  {
    if (this.input1 != null) 
	throw new EdgeLinkageError("input #1 already set");
    this.input1 = input1;
  }
  @Override
  public void setInput2(ControlEdge input2) 
      throws EdgeLinkageError
  {
    if (this.input2 != null)
	throw new EdgeLinkageError("input #2 already set");
    this.input2 = input2;
  }
  @Override
  public void setOutput(ControlEdge output)
    throws EdgeLinkageError
  {
    if (this.output != null) throw new EdgeLinkageError("output already set");
    this.output = output;
  }
}
