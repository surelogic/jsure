package com.surelogic.xml;

abstract class AbstractJavaElement implements IJavaElement {
	private IJavaElement parent;
	private boolean isDirty;
	
	@Override
	public final IJavaElement getParent() {
		return parent;
	}

	@Override
	public final void setParent(IJavaElement p) {
		if (parent != null) {
			throw new IllegalStateException("Already has a parent");
		}
		parent = p;
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
}
