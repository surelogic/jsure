package com.surelogic.xml;

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
	
	public void modify(String value) {
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
}
