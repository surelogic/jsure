package com.surelogic.xml;

public interface IMergeableElement extends IJavaElement {
	boolean isToBeDeleted();
	boolean isModified();
	int getRevision();
	void incrRevision();
	
	/**
	 * Also include anything attached to me
	 */
	IMergeableElement cloneMe();
	
	/**
	 * Merge anything attached to either,
	 * leaving this element otherwise unchanged
	 */
	void mergeAttached(IMergeableElement other);
}
