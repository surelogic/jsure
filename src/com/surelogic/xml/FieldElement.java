package com.surelogic.xml;

public class FieldElement extends AbstractJavaElement implements IClassMember {
	FieldElement(String id) {
		super(id);
	}

	@Override
	public String getLabel() {
		return "Field "+getName();
	}
	
	@Override
	public final String getImageKey() {
		return null; // TODO
	}
}
