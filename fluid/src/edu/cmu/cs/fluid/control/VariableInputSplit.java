package edu.cmu.cs.fluid.control;

/**
 * @author Scott Wisniewski
 *
 */
public class VariableInputSplit extends VariableInputControlNode implements TwoOutput {
	
	private ControlEdge output1_;
	private ControlEdge output2_;
	
	/** 
	 * @see edu.cmu.cs.fluid.control.TwoOutput#getOutput1()
	 */
	@Override
  public ControlEdge getOutput1() {
		return output1_;
	}
	
	/** 
	 * @see edu.cmu.cs.fluid.control.TwoOutput#getOutput2()
	 */
	@Override
  public ControlEdge getOutput2() {
		return output2_;
	}
	
	/** 
	 * @see edu.cmu.cs.fluid.control.TwoOutput#setOutput1(edu.cmu.cs.fluid.control.ControlEdge)
	 */
	@Override
  public void setOutput1(ControlEdge output1) {
		output1_ = output1;
	}
	
	/** 
	 * @see edu.cmu.cs.fluid.control.TwoOutput#setOutput2(edu.cmu.cs.fluid.control.ControlEdge)
	 */
	@Override
  public void setOutput2(ControlEdge output2) {
		output2_ = output2;
	}
	
        @Override
        public ControlEdge getOutput(boolean secondary) {
          return secondary?output2_:output1_;
        }
        
	/** 
	 * @see edu.cmu.cs.fluid.control.ControlNode#getOutputs()
	 */
	@Override
  public ControlEdgeIterator getOutputs() {
		return getVariableOutputs();
	}
	
	/**
	 * @see edu.cmu.cs.fluid.control.VariableInputControlNode#getVariableOutputs()
	 */
	@Override
  public VariableInputControlEdgeIterator getVariableOutputs() {
		return new AdaptedVariableInputControlEdgeIterator( new PairControlEdgeIterator(output1_, output2_));
	}
}
