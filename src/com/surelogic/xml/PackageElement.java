package com.surelogic.xml;

import java.util.List;

public class PackageElement extends AbstractJavaElement {
	private final ClassElement clazz;
	
	public PackageElement(String id, ClassElement c) {
		super(id);
		clazz = c;
		c.setParent(this);
	}	
	
	public final ClassElement getClassElement() {
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
		return clazz.isDirty() || super.isDirty();
	}
	
	public void markAsClean() {
		super.markAsClean();
		clazz.markAsClean();
	}
}
