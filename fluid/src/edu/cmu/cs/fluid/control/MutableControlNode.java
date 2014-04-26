package edu.cmu.cs.fluid.control;

/**
 * An interface for mutating CFG structure before analysis starts.
 * This interface is dangerous.
 * @author boyland
 */
interface MutableControlNode {
	/**
	 * Remove an incoming edge.  Replace with null.
	 * This method is dangerous.  Call only while preparing a CFG before any analysis.
	 * @param e edge to remove, must not be null
	 * @throws EdgeLinkageError if this edge wasn't incoming
	 */
	void resetInput(ControlEdge e);
	
	/**
	 * Remove an output edge.  Replace with null.
	 * This method is dangerous.  Call only while preparing a CFG before any analysis.
	 * @param e edge to remove, must not be null
	 * @throws EdgeLinkageError if this edge wasn't incoming
	 */
	void resetOutput(ControlEdge e);
}
