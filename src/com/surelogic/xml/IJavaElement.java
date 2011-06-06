package com.surelogic.xml;

/**
 * Mostly for use by a Content/LabelProvider
 * 
 * @author Edwin
 */
public interface IJavaElement {
	String getLabel();
	boolean hasChildren();
	Object[] getChildren();
}
