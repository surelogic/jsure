package com.surelogic.xml;

public class PackageElement extends AbstractJavaElement {
	private final ClassElement clazz;
	
	PackageElement(String id, ClassElement c) {
		super(id);
		clazz = c;
	}	
	
	final ClassElement getClassElement() {
		return clazz;
	}
}
