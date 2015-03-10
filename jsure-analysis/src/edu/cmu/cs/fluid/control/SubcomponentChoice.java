/* Header */
package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

/** Control-flow decision points in the graph that depend
 * on language-specific conditions.  The subcomponent is identified
 * as well as an arbitrary value that may be used to distinguish
 * multiple control points within a subcomponent.
 * This node is treated as a ComponentChoice during control flow analysis.
 * @see ComponentChoice
 */
public class SubcomponentChoice extends Choice implements MutableSubcomponentNode {
	ISubcomponent sub;
	public SubcomponentChoice(ISubcomponent s, Object v) {
		super(v);
		sub = s;
		sub.registerSubcomponentNode(this);
	}
	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.control.SubcomponentNode#getSubcomponent()
	 */
	@Override
	public ISubcomponent getSubcomponent() {
		return sub;
	}
	
	@Override
	public void setSubcomponent(ISubcomponent s) {
		sub = s;
	}
	
	@Override
	public IRNode getSyntax() {
		return sub.getComponent().getSyntax();
	}
}
