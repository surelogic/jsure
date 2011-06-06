package com.surelogic.xml;

public class ClassInitElement extends AbstractJavaElement implements IClassMember {
	ClassInitElement() {
		super("classinit");
	}

	@Override
	public String getLabel() {
		return "<clinit>";
	}
}
