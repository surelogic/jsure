package edu.cmu.cs.fluid.control;

/**
 * Interface for changing structure of some CFG nodes.
 * This interface should only be used before a CFG has been registered
 * or used for analysis.  Afterwards, analysis results will be confused.
 * @author boyland
 */
interface MutableSubcomponentNode {
	/**
	 * Change the subcomponent that this node is assigned to.
	 * This operation is dangerous.
	 * @param sub
	 */
	void setSubcomponent(ISubcomponent sub);
}
