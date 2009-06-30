/*
 * Created on Jul 15, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.LocationGenerator.SimpleLocation;

/**
 * Interface for lattices that map variables and other expressions to
 * locations. Used within IWACO-style analysis, and probably not much 
 * use elsewhere.
 */
@Deprecated
public interface ILocationMap extends Lattice {
	/**
	 * Return a new ILocationMap with the location for the appropriate 
	 * (local) variable updated.
	 * @param decl --The declaration of the variable to update.
	 * @param loc --The location to which the variable will point 
	 * @return The new map containing these changes.
	 */
	public abstract ILocationMap replaceLocation(IRNode decl, 
				SimpleLocation loc);

	/**
	 * Return a new ILocationMap with the location for the appropriate
	 * field (determined by a location, field declaration pair) updated.
	 * @param obj -- The location of the object whose field is being 
	 * 														updated
	 * @param fieldDecl -- The field to update
	 * @param loc -- the location to which the field will now point
	 * @return The new map containing these changes.
	 */	
	public abstract ILocationMap replaceLocation(SimpleLocation obj,
				IRNode fieldDecl, SimpleLocation loc);

	public abstract ILocationMap renameLocation(IRNode expr);
	public abstract ILocationMap renameLocation(LocationField lf);
	
	/**
	 * Retrieve the abstract location to which the expr evaluates. 
	 * @param expr An Expression IRNode
	 * @return The abstract location to which the expr evaluates. 
	 */
  @Deprecated
	public abstract SimpleLocation getLocation(IRNode expr);

	/**
	 * Retrieve the abstract location the field references
	 * @return
	 */
  @Deprecated
	public abstract SimpleLocation getLocation(LocationField lf);
	/**
	 * @return The <tt>null</tt> location.
	 */
  @Deprecated
	public abstract SimpleLocation nulLoc();
}
