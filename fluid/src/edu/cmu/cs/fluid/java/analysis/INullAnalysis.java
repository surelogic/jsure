/*
 * Created on Jul 17, 2003
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;

public interface INullAnalysis {
	/** Return true if the following expression (in context) may
	 * ever evaluate to null.  
	 * @param expr an executable expression node in a Java class.
	 */
	public boolean maybeNull(IRNode expr, IRNode constructorContext);

}
