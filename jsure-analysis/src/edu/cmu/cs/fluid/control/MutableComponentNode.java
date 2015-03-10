package edu.cmu.cs.fluid.control;

/**
 * Interface for changing some CFG nodes.
 * This interface is dangerous and should only be used to change
 * the structure of a CFG before it has been registered or used for analysis.
 * @author boyland
 */
interface MutableComponentNode {
	/**
	 * Change the component of this node before analysis starts.
	 * Normally, this should never be called.
	 * It should never be called if the component for this node
	 * is already registered.
	 * @param c component
	 */
	public void setComponent(Component c);
}
