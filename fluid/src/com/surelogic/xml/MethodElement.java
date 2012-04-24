package com.surelogic.xml;

import com.surelogic.common.xml.Entity;

import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class MethodElement extends AbstractFunctionElement {
	public MethodElement(String id, boolean isPublic, String params) {
		super(id, isPublic, params);
	}
	MethodElement(String id, Entity e) {
		super(id, e);
	}

	public String getLabel() {
		return getName()+"("+getParams()+")";
	}
	
	@Override
	public Operator getOperator() {
		return MethodDeclaration.prototype;
	}
	
	@Override
	MethodElement cloneMe(IJavaElement parent) {
		MethodElement clone = new MethodElement(getName(), isPublic(), getParams());
		copyToClone(clone);
		return clone;
	}
	
	MethodElement copyIfDirty() {
		if (isDirty()) {
			MethodElement clone = new MethodElement(getName(), isPublic(), getParams());
			copyIfDirty(clone);
			return clone;
		}
		return null;
	}
	
	public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
}
