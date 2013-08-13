package edu.cmu.cs.fluid.java.bind;

import java.util.List;

/**
 * A representation of a function type.
 * @see ITypeEnvironment.isFunctionalType
 */
public interface IJavaFunctionType {
	/**
	 * Return an immutable list of the formal parameter types
	 * @return formal parameter types
	 */
	public List<IJavaType> getParameterTypes();
	
	/**
	 * Get the return type of this function type
	 * @return return type of this function type
	 */
	public IJavaType getReturnType();
	
	/**
	 * Return whether this function type accepts variable parameters.
	 * If so, there will be at least on parameter type,
	 * and the last type will be an array type of some sort.
	 * @return whether this function accepts variable arguments.
	 */
	public boolean isVariable();
}
