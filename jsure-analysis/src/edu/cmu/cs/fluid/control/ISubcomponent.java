package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

public interface ISubcomponent {

	/** Return the component context (component of parent). */
	public abstract Component getComponent();

	/** Return the location of the child for this subcomponent */
	public abstract IRLocation getLocation();

	/** Return the component which is the dual to this. */
	public abstract Component getComponentInChild();

	/** Return the node this subcomponent wraps. */
	public abstract IRNode getSyntax();

	/** Return the start port. */
	public abstract Port getEntryPort();

	/** Return the normal exit port. */
	public abstract Port getNormalExitPort();

	/** Return the abrupt exit port */
	public abstract Port getAbruptExitPort();

	/** Return the entering or exiting variable edge for the following index.
	 * (Overridden in VariableSubcomponent.)
	 * @param isEntry
	 * If true, then return the entry edge for the index,
	 * otherwise return the exit edge for the index.
	 */
	public abstract VariableSubcomponentControlEdge getVariableEdge(int index,
			boolean isEntry);

	void registerSubcomponentNode(SubcomponentNode node);
}