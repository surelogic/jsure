package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

public interface BiComponentFactory extends ComponentFactory {
	public BiComponent getBiComponent(IRNode syntax, boolean quiet);

	/**
	 * Remove all registrations for deleted IRNodes.
	 * This method is recommended after deleting IRNodes.
	 * @see IRNode#destroy()
	 */
	public void clean();
	
	/**
	 * Remove <em>all</em> registrations of BiComponents.
	 * This action is drastic.
	 */
	public void clear();
}
