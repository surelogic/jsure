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

	ConstructorElement cloneMe() {
		ConstructorElement clone = new ConstructorElement(getParams());
		copyToClone(clone);
		return clone;
	}
}
