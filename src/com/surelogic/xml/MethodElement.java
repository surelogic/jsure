package com.surelogic.xml;

import com.surelogic.common.xml.Entity;

import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class MethodElement extends AbstractFunctionElement {
	public MethodElement(String id, String params) {
		super(id, params);
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
}
