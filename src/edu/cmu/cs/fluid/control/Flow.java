/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/Flow.java,v 1.12 2005/05/20 15:48:03 chance Exp $ */
package edu.cmu.cs.fluid.control;


/** Abstract class for control-flow nodes with one input and one output
 * @author John Tang Boyland
 * @see NoOperation
 * @see ComponentFlow
 * @see SubcomponentFlow
 * @see AddLabel
 * @see PendingLabelStrip
 */

public abstract class Flow extends Entity
     implements ControlNode, OneInput, OneOutput
{
  protected ControlEdge input, output;
  public ControlEdge getInput() { return input; }
  public ControlEdge getOutput() { return output; }
  public void setInput(ControlEdge input) 
      throws EdgeLinkageError
  {
    if (this.input != null) throw new EdgeLinkageError("input already set");
    this.input = input;
  }
  public void setOutput(ControlEdge output)
    throws EdgeLinkageError
  {
    if (this.output != null) throw new EdgeLinkageError("output already set");
    this.output = output;
  }
  public ControlEdgeIterator getInputs() {
    return new SingleControlEdgeIterator(input);
  }
  public ControlEdgeIterator getOutputs() {
    return new SingleControlEdgeIterator(output);
  }
}
