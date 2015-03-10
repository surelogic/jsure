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
public interface ISubTypeSearchStrategy<T> extends ITypeSearchStrategy<T> {
	/**
	 * @param type The type to search
	 */
	void visitClass(IRNode type);

	/**
	 * @return Whether to continue searching the subclasses of the last class visited
	 */
	boolean visitSubclasses();

	/**
	 * @param type
	 */
	void visitInterface(IRNode type);
}
