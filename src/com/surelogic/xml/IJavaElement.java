package com.surelogic.xml;

/**
 * Mostly for use by a Content/LabelProvider
 * 
 * @author Edwin
 */
public interface IJavaElement {
	void setParent(AbstractJavaElement p);
	AbstractJavaElement getParent();
	
	String getImageKey();
	String getLabel();
	boolean hasChildren();
	Object[] getChildren();
}
