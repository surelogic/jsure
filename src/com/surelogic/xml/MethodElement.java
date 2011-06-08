package com.surelogic.xml;

import com.surelogic.common.xml.Entity;

public class MethodElement extends AbstractFunctionElement {
	public MethodElement(String id, String params) {
		super(id, params);
	}
	MethodElement(String id, Entity e) {
		super(id, e);
	}
	
	@Override
	public String getLabel() {
		return getName()+"("+getParams()+")";
	}
}
