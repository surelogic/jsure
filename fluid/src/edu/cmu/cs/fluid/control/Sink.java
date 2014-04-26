/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/Sink.java,v 1.10 2005/05/20 15:48:03 chance Exp $ */
package edu.cmu.cs.fluid.control;


/** Abstract class for control-flow nodes with one input and no output
 * @author John Tang Boyland
 * @see Abort
 * @see ComponentSink
 */

public abstract class Sink extends Entity
     implements ControlNode, OneInput, NoOutput, MutableControlNode
{
  protected ControlEdge input;
  @Override
  public ControlEdge getInput() { return input; }
  @Override
  public void setInput(ControlEdge input) 
      throws EdgeLinkageError
  {
    if (this.input != null) throw new EdgeLinkageError("input already set");
    this.input = input;
  }
  @Override
  public ControlEdgeIterator getInputs() {
    return new SingleControlEdgeIterator(input);
  }
  @Override
  public ControlEdgeIterator getOutputs() {
    return EmptyControlEdgeIterator.prototype;
  }
  
  @Override
  public void resetInput(ControlEdge e) {
	 if (input == e) input = null;
	 else throw new EdgeLinkageError("not an outgoing edge: " + e);
  }
  @Override
  public void resetOutput(ControlEdge e) {
	  throw new EdgeLinkageError("not an incoming edge: " + e);
  }
}
