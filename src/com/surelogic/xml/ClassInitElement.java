package com.surelogic.xml;

public class ClassInitElement extends AnnotatedJavaElement implements IClassMember {
	public ClassInitElement() {
		super("classinit");
	}

	public String getLabel() {
		return "<clinit>";
	}
	
	public final String getImageKey() {
		return null; // TODO
	}
}
