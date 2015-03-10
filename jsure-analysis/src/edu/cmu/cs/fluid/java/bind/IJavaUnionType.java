package edu.cmu.cs.fluid.java.bind;

/**
 * A type that could be one of two different types:
 * one primary, the other secondary.
 * 
 * This type may appear in the declaration of catch clauses
 * @author edwin
 */
public interface IJavaUnionType extends IJavaReferenceType {
	public IJavaReferenceType getFirstType();

	public IJavaReferenceType getAlternateType();
}
