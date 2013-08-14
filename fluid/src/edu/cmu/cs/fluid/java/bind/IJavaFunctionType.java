package edu.cmu.cs.fluid.java.bind;

import java.util.List;
import java.util.Set;

/**
 * A representation of a function type.
 * @see ITypeEnvironment.isFunctionalType
 */
public interface IJavaFunctionType {
	/**
	 * Return an immutable list of the type formals.
	 * If this function is polymorphic, the result will not be empty.
	 * Otherwise the list will be empty, not null.
	 * @return list of the type formals for this function.
	 */
	public List<IJavaTypeFormal> getTypeFormals();
	
	/**
	 * Return an immutable list of the formal parameter types
	 * @return formal parameter types
	 */
	public List<IJavaType> getParameterTypes();
	
	/**
	 * Return an immutable set of the exception types (throws) for this method.
	 * The set may be empty, but will not be null.
	 * @return set of exception types that could be thrown. 
	 */
	public Set<IJavaType> getExceptions();
	
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
	
	/// We implement some methods similar to that in IJavaType:
	
	/**
	 * Get an unparse similar to what would appear in source code
	 * (e.g., relative names omitting package names)
	 */
	public String toSourceText();

	/**
	 * Produce a new type in which any type parameters are substituted
	 * by the given substitution
	 * @param s the substitution to use for types in the function type..
	 *    This substitution may not apply to the type formals of this function type.
	 * @return new type (that is identical if there was no change).
	 */
	public IJavaFunctionType subst(IJavaTypeSubstitution s);

	/**
	 * Produce a new function type in which the old type formals
	 * are substituted away.
	 * @param newFormals new formals to use in the function type.
	 * @param s substitution of old type formals (and perhaps others too).
	 * @return new type (that may be identical if there were no changes).
	 */
	public IJavaFunctionType instantiate(List<IJavaTypeFormal> newFormals, IJavaTypeSubstitution s);
	
	/**
	 * @return true if it and all the types it depends on are valid
	 */
	public boolean isValid();
}
