package com.surelogic.xml;

public class FieldElement extends AnnotatedJavaElement implements IClassMember {
	FieldElement(String id) {
		super(id);
	}

	public String getLabel() {
		return "Field "+getName();
	}
	
	public final String getImageKey() {
		return null; // TODO
	}
}
