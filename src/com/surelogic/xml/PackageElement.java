package com.surelogic.xml;

import java.util.List;

import com.surelogic.common.CommonImages;

public class PackageElement extends AnnotatedJavaElement {
	private final ClassElement clazz;
	
	public PackageElement(String id, ClassElement c) {
		super(id);
		clazz = c;
		c.setParent(this);
	}	

	public final String getImageKey() {
		return CommonImages.IMG_PACKAGE;
	}
	
	public final ClassElement getClassElement() {
		return clazz;
	}

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
