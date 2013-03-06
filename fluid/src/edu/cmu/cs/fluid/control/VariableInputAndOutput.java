package edu.cmu.cs.fluid.control;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Scott Wisniewski
 */
public class VariableInputAndOutput extends VariableInputControlNode implements VariableOutput {
	
	private Map<ControlNode,ControlEdge> outputEdgeMap_;
	private ControlEdge primaryOutputEdge_;
	
	
	public VariableInputAndOutput() {
		outputEdgeMap_ = new TreeMap<ControlNode,ControlEdge>();
	}
	
	/**
	 * @see edu.cmu.cs.fluid.control.VariableOutput#indicateOutputEdge(ControlNode, edu.cmu.cs.fluid.control.ControlEdge)
	 */
	@Override
  public void indicateOutputEdge(ControlNode n, ControlEdge e) {
		outputEdgeMap_.put(n, e);
	}
	
	@Override
  public ControlEdgeIterator getOutputs() {
		return getVariableOutputs();
	}
	
	@Override public VariableInputControlEdgeIterator getVariableOutputs() {
		return new GeneralControlEdgeIterator(outputEdgeMap_.values().iterator());
	}
	
	/**
	 * @see edu.cmu.cs.fluid.control.VariableOutput#hasOutputEdge(edu.cmu.cs.fluid.control.ControlNode)
	 */
	@Override
  public boolean hasOutputEdge(ControlNode n) {
		return outputEdgeMap_.containsKey(n);
		
	}
	
	/**
	 * @see edu.cmu.cs.fluid.control.VariableOutput#getPrimaryOutputEdge()
	 */
	@Override
  public ControlEdge getPrimaryOutputEdge() {
		return primaryOutputEdge_;
	}
	
	/**
	 * @see edu.cmu.cs.fluid.control.VariableOutput#setPrimaryEdge(edu.cmu.cs.fluid.control.ControlEdge)
	 */
	@Override
  public void setPrimaryEdge(ControlEdge e) {
		primaryOutputEdge_ = e;
	}
}
