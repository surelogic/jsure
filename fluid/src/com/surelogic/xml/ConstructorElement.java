package com.surelogic.xml;

import com.surelogic.dropsea.irfree.Entity;

import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class ConstructorElement extends AbstractFunctionElement {
	public ConstructorElement(boolean confirmed, Access access, boolean isStatic, String params) {
		super(confirmed, "new", access, isStatic, params);
	}
	
	ConstructorElement(Entity e) {
		super("new", e);
	}

	public String getLabel() {
		return "Constructor("+getParams()+")";
	}
	
	@Override
	public Operator getOperator() {
		return ConstructorDeclaration.prototype;
	}

	@Override
	ConstructorElement cloneMe(IJavaElement parent) {
		ConstructorElement clone = new ConstructorElement(isConfirmed(), getAccessibility(), isStatic(), getParams());
		copyToClone(clone);
		return clone;
	}
	
	ConstructorElement copyIfDirty() {
		if (isDirty()) {
			ConstructorElement clone = new ConstructorElement(isConfirmed(), getAccessibility(), isStatic(), getParams());
			copyIfDirty(clone);
			return clone;
		}
		return null;
	}
	
	public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
}
