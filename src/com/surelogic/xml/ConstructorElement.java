package com.surelogic.xml;

import com.surelogic.common.xml.Entity;

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
}
