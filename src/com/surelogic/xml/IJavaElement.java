package com.surelogic.xml;

/**
 * Mostly for use by a Content/LabelProvider
 * 
 * @author Edwin
 */
public interface IJavaElement {
	void setParent(IJavaElement p);
	IJavaElement getParent();
	
	String getImageKey();
	String getLabel();
	boolean hasChildren();
	Object[] getChildren();
	
	boolean canModify();
	void modify(String value);
}
