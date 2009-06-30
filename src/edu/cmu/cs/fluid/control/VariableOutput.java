package edu.cmu.cs.fluid.control;

/**
 * Defines an interface for ControlNodes that may
 * have a variable number of outputs.
 * @author Scott Wisniewski
 */
public interface VariableOutput {
	void indicateOutputEdge(ControlNode n, ControlEdge e);
	boolean hasOutputEdge(ControlNode n);
	ControlEdge getPrimaryOutputEdge();
	void setPrimaryEdge(ControlEdge e);
}
