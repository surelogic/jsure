package com.surelogic.jsure.client.eclipse.refactor;

/**
 * IJavaDeclaration elements are expected to obey hashCode and equals
 * 
 * @author nathan
 * 
 */
public interface IJavaDeclaration {
	/**
	 * The type context that the current java declaration is defined in. In the
	 * case of a type declaration, this is most likely itself.
	 * 
	 * @return the type context of this declaration
	 */
	TypeContext getTypeContext();

	/**
	 * Returns a description of this Java declaration in for syntax used by
	 * {@link com.surelogic.Assume}. NOTE: this should not include a package
	 * declaration.
	 * 
	 * @return
	 */
	String forSyntax();
}
