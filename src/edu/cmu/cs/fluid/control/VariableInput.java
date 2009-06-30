package edu.cmu.cs.fluid.control;

/**
 * Defines an interface for ControlNodes
 * that may have an arbitrary number of input
 * edges.  
 * @author Scott Wisniewski
 */
public interface VariableInput {
	void indicateInputEdge(ControlNode n, ControlEdge e);
	boolean hasInputEdge(ControlNode n);
}
