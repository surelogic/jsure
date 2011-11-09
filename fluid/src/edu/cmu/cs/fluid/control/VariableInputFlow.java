package edu.cmu.cs.fluid.control;


/**
 * @author Scott Wisniewski
 *
 */
public class VariableInputFlow extends VariableInputControlNode implements OneOutput {
	
	private ControlEdge output_;
	
	/*
	 * @see edu.cmu.cs.fluid.control.OneOutput#getOutput()
	 */
	public ControlEdge getOutput() {
		return output_;
	}
	
	/** 
	 * @see edu.cmu.cs.fluid.control.OneOutput#setOutput(edu.cmu.cs.fluid.control.ControlEdge)
	 */
	public void setOutput(ControlEdge output) {
		output_ = output;
		
	}
	
	/** 
	 * @see edu.cmu.cs.fluid.control.ControlNode#getOutputs()
	 */
	public ControlEdgeIterator getOutputs() {
		return getVariableOutputs();
	}
	
	/**
	 * @see edu.cmu.cs.fluid.control.VariableInputControlNode#getVariableOutputs()
	 */
	@Override
  public VariableInputControlEdgeIterator getVariableOutputs() {
		return new AdaptedVariableInputControlEdgeIterator(new SingleControlEdgeIterator(output_));
	}
}
