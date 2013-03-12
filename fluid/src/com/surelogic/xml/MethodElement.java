package com.surelogic.xml;

import com.surelogic.common.xml.Entity;

import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class MethodElement extends AbstractFunctionElement {
	public MethodElement(boolean confirmed, String id, Access access, boolean isStatic, String params) {
		super(confirmed, id, access, isStatic, params);
	}
	MethodElement(String id, Entity e) {
		super(id, e);
	}

	@Override
  public String getLabel() {
		return getName()+"("+getParams()+")";
	}
	
	@Override
	public Operator getOperator() {
		return MethodDeclaration.prototype;
	}
	
	@Override
	MethodElement cloneMe(IJavaElement parent) {
		MethodElement clone = new MethodElement(isConfirmed(), getName(), getAccessibility(), isStatic(), getParams());
		copyToClone(clone);
		return clone;
	}
	
	MethodElement copyIfDirty() {
		if (isDirty()) {
			MethodElement clone = new MethodElement(isConfirmed(), getName(), getAccessibility(), isStatic(), getParams());
			copyIfDirty(clone);
			return clone;
		}
		return null;
	}
	
	@Override
  public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
}
