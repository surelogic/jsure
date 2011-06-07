package com.surelogic.xml;

import java.util.List;

public class PackageElement extends AbstractJavaElement {
	private final ClassElement clazz;
	
	PackageElement(String id, ClassElement c) {
		super(id);
		clazz = c;
	}	
	
	final ClassElement getClassElement() {
		return clazz;
	}

	@Override
	public String getLabel() {
		return "package "+getName();
	}
	
	@Override
	public boolean hasChildren() {
		return super.hasChildren() || clazz != null;
	}
	
	@Override
	protected void collectOtherChildren(List<Object> children) {
		if (clazz != null) {
			children.add(clazz);
		}
	}

	public boolean isDirty() {
		return isDirty();
	}
	
	public void markAsClean() {
		// TODO Auto-generated method stub
		
	}
}
