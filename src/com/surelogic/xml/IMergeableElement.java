package com.surelogic.xml;

public interface IMergeableElement extends IJavaElement {
	boolean isModified();
	int getRevision();
	void incrRevision();
}
