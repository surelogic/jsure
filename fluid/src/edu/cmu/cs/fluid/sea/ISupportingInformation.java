/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;

public interface ISupportingInformation {
	/**
	 * @return the fAST location this supporting information references, can
	 *   be <code>null</code>
	 */
	public IRNode getLocation();

	/**
	 * @return a message describing the point of this supporting information
	 */
	public String getMessage();

	/**
	 * @return the source reference of the fAST node this information
	 *   references, can be <code>null</code>
	 */
	public ISrcRef getSrcRef();

	public boolean sameAs(IRNode link, int num, Object[] args);
	
	public boolean sameAs(IRNode link, String message);
}
