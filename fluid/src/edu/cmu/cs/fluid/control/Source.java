/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/Source.java,v 1.10 2005/05/20 15:48:03 chance Exp $ */
package edu.cmu.cs.fluid.control;


/** Abstract class for control-flow nodes with no input and one output
 * @author John Tang Boyland
 * @see Never
 * @see ComponentSource
 */

public abstract class Source extends Entity
     implements ControlNode, NoInput, OneOutput, MutableControlNode
{
  protected ControlEdge output;
  @Override
  public ControlEdge getOutput() { return output; }
  @Override
  public void setOutput(ControlEdge output)
    throws EdgeLinkageError
  {
    if (this.output != null) throw new EdgeLinkageError("output already set");
    this.output = output;
  }
  @Override
  public ControlEdgeIterator getInputs() {
    return EmptyControlEdgeIterator.prototype;
  }
  @Override
  public ControlEdgeIterator getOutputs() {
    return new SingleControlEdgeIterator(output);
  }
  
  @Override
  public void resetInput(ControlEdge e) {
	 throw new EdgeLinkageError("not an outgoing edge: " + e);
  }
  @Override
  public void resetOutput(ControlEdge e) {
	  if (output != null && output.equals(e)) output = null;
	  else throw new EdgeLinkageError("not an incoming edge: " + e);
  }
}
