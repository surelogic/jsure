package com.surelogic.xml;

import com.surelogic.common.xml.Entity;

public class ConstructorElement extends AbstractFunctionElement {
	ConstructorElement(Entity e) {
		super("new", e);
	}

	@Override
	public String getLabel() {
		return "Constructor("+getParams()+")";
	}
}
