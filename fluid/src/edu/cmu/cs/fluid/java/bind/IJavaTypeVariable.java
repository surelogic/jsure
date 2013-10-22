package edu.cmu.cs.fluid.java.bind;

/**
 * An interface to unify type formals and capture types
 * 
 * @author edwin
 */
public interface IJavaTypeVariable extends IJavaType {
	/** Get the least upper bound (if any), e.g. ? extends X, 
	 * by combining the upper bounds from the wildcard type and the type parameter.
	 * (See JLS 7 page 63)
	 *  
	 * @return upper bound (or null, if none)
	 * @see com.surelogic.ast.ICaptureType#getUpperBound()
	 */
	public IJavaReferenceType getUpperBound(ITypeEnvironment te);
}
