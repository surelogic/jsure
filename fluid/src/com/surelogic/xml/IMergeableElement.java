package com.surelogic.xml;

public interface IMergeableElement extends IJavaElement {

	/**
	 * Indicates that this is a reference node simply included to preserve the
	 * order of an annotation on a particular declaration.
	 * 
	 * @return <tt>true</tt> if this is an unchanged node used to preserve
	 *         order, <tt>false</tt> otherwise.
	 */
	boolean isReference();

	/**
	 * Indicates that this node is marked to be deleted when its merged with
	 * Fluid.
	 * 
	 * @return <tt>true</tt> if this node is to be deleted upon merge with
	 *         Fluid, <tt>false</tt> otherwise.
	 */
	boolean isToBeDeleted();

	/**
	 * Deletes this element from the tree.
	 * 
	 * @return <tt>true</tt> if this was created and then deleted.
	 */
	boolean delete();

	/**
	 * Also include anything attached to me
	 */
	IMergeableElement cloneMe(IJavaElement parent);

	/**
	 * Merge anything attached to either, leaving this element otherwise
	 * unchanged
	 */
	void mergeAttached(IMergeableElement other);
}
