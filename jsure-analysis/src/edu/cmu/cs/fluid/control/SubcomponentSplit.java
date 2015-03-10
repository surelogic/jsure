/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/SubcomponentSplit.java,v 1.2 2003/07/02 20:19:22 thallora Exp $ */
package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

/** A control flow node that itself can decide whether flow goes
 * to either output depending on the syntax of a subcomponent.
 */
public abstract class SubcomponentSplit extends Split implements MutableSubcomponentNode {
	public SubcomponentSplit(ISubcomponent s) { 
		super(); 
		sub = s;
		sub.registerSubcomponentNode(this);
	}
	
	/** Return true if flow can happen between the source
	 * and the output indicated.
	 * @param flag true if first output, false if second output.
	 */
	public abstract boolean test(boolean flag);
	
	private ISubcomponent sub;
	
	@Override
	public ISubcomponent getSubcomponent() {
		return sub;
	}
	
	@Override
	public void setSubcomponent(ISubcomponent s) {
		sub = s;
	}
	
	public IRNode getSyntax() {
		return sub.getSyntax();
	}
}
