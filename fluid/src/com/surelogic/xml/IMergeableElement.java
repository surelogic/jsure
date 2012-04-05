package com.surelogic.xml;

public interface IMergeableElement extends IJavaElement {
	boolean isReference();	
	boolean isToBeDeleted();
	boolean isModified();
	int getRevision();
	void incrRevision();
	boolean delete();
	
	/**
	 * Also include anything attached to me
	 */
	IMergeableElement cloneMe(IJavaElement parent);
	
	/**
	 * Merge anything attached to either,
	 * leaving this element otherwise unchanged
	 */
	void mergeAttached(IMergeableElement other);

}
