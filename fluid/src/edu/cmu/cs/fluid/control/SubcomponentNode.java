package edu.cmu.cs.fluid.control;

/**
 * A flow node with semantics dependent on a child of a node in the CFG.
 */
public interface SubcomponentNode {

	/**
	 * Return the subcomponent associated with this flow node.
	 * @return
	 */
	public abstract ISubcomponent getSubcomponent();

}