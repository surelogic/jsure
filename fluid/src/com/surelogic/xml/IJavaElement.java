package com.surelogic.xml;

import com.surelogic.common.logging.IErrorListener;

import edu.cmu.cs.fluid.tree.Operator;

/**
 * Mostly for use by a Content/LabelProvider
 * 
 * @author Edwin
 */
public interface IJavaElement {
	<T> T visit(IJavaElementVisitor<T> v);
	void setParent(IJavaElement p);
	IJavaElement getParent();
	
	String getImageKey();
	String getLabel();
	boolean hasChildren();
	Object[] getChildren();
	/**
	 * Has unsaved changes
	 */
	boolean isDirty();
	
	/**
	 * e.g. because the syntax is wrong
	 */
	boolean isBad();
	/**
	 * Has any changes
	 */
	boolean isModified();
	boolean canModify();
	boolean modify(String value, IErrorListener l);
	Operator getOperator();


}
