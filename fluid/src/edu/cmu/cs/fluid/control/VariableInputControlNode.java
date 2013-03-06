package edu.cmu.cs.fluid.control;


import java.util.TreeMap;
import java.util.Map;

import edu.cmu.cs.fluid.ir.PlainIRNode;

/**
 * @author Scott Wisniewski
 *
 */
public abstract class VariableInputControlNode extends PlainIRNode implements ControlNode, VariableInput {
	
	private Map<ControlNode,ControlEdge> inputEdgeMap_;
	
	public static final String NODE_TYPE_NOT_SUPPORTED = "The indicated node is of a unsupported type";
	public static final String EDGE_TYPE_NOT_SUPPORTED= "The indicated edge is of an unsupported type";
	
	
	public VariableInputControlNode() {
		inputEdgeMap_ = new TreeMap<ControlNode,ControlEdge>();
	}
	
	/** 
	 * @see edu.cmu.cs.fluid.control.ControlNode#getInputs()
	 */
	@Override
  public ControlEdgeIterator getInputs() {
		return getVariableInputs();
	}
	
	/**
	 * Returns a collection of the edges that point into this node
	 * as a VariableInputControlEdgeIterator 
	 * @return
	 */
	public VariableInputControlEdgeIterator getVariableInputs() {
		return new GeneralControlEdgeIterator(inputEdgeMap_.values().iterator());
	}
	
	/**
	 * @see edu.cmu.cs.fluid.control.VariableInput#indicateInputEdge(ControlNode, edu.cmu.cs.fluid.control.ControlEdge)
	 */
	@Override
  public void indicateInputEdge(ControlNode n, ControlEdge e) {
		if (! (n instanceof VariableInput))
			throw new EdgeLinkageError(NODE_TYPE_NOT_SUPPORTED);
		else if (! (e instanceof VariableInputControlEdge))
			throw new EdgeLinkageError(EDGE_TYPE_NOT_SUPPORTED);
		else
			inputEdgeMap_.put(n,e);
	}
	
	/**
	 * @see edu.cmu.cs.fluid.control.VariableInput#hasInputEdge(edu.cmu.cs.fluid.control.ControlNode)
	 */
	@Override
  public boolean hasInputEdge(ControlNode n) {
		return inputEdgeMap_.containsKey(n);
	}
	
	public abstract VariableInputControlEdgeIterator getVariableOutputs();
}
