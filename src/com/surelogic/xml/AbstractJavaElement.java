package com.surelogic.xml;

import com.surelogic.common.logging.IErrorListener;

abstract class AbstractJavaElement implements IJavaElement {
	private IJavaElement parent;
	private boolean isDirty;
	
	public final IJavaElement getParent() {
		return parent;
	}

	public final void setParent(IJavaElement p) {
		if (parent != null) {
			throw new IllegalStateException("Already has a parent");
		}
		parent = p;
	}
	
	public boolean canModify() {
		return false;
	}
	
	public void modify(String value, IErrorListener l) {
		throw new UnsupportedOperationException();
	}
	
	boolean isDirty() {
		return isDirty;
	}

	void markAsDirty() {
		isDirty = true;
	}
	
	void markAsClean() {
		isDirty = false;
	}
	
	void mergeThis(AbstractJavaElement changed, MergeType type) {
		// Nothing to do yet
	}
	
	void copyToClone(CommentedJavaElement clone) {
		// Nothing to do yet
	}
}
