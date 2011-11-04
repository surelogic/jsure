package com.surelogic.xml;

import com.surelogic.common.xml.Entity;

import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class ConstructorElement extends AbstractFunctionElement {
	public ConstructorElement(String params) {
		super("new", params);
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
	ConstructorElement cloneMe() {
		ConstructorElement clone = new ConstructorElement(getParams());
		copyToClone(clone);
		return clone;
	}
	
	ConstructorElement copyIfDirty() {
		if (isDirty()) {
			ConstructorElement clone = new ConstructorElement(getParams());
			copyIfDirty(clone);
			return clone;
		}
		return null;
	}
	
	public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
}
