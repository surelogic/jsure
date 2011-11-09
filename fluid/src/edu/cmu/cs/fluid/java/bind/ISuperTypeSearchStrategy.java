/*
 * Created on Dec 17, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * @author chance
 *
 */
public interface ISuperTypeSearchStrategy<T> extends ITypeSearchStrategy<T> {
	/**
	 * @param type The type to search
	 */
	void visitClass(IRNode type);

	/**
	 * @param type The type to search
	 */	
	void visitInterface(IRNode type);		

	/**
	 * @return Whether to continue searching the superclass of the last class visited
	 */
	boolean visitSuperclass();

	/**
	 * @return Whether to continue searching the superinterfaces of the last class visited
	 */	
	boolean visitSuperifaces();
	
	void reset();
}
