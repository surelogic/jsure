package com.surelogic.xml;

public class NestedClassElement extends ClassElement implements IClassMember {
	public NestedClassElement(String id, boolean isPublic) {
		super(id, isPublic);
	}

	@Override
	public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
	
	@Override
	NestedClassElement cloneMe(IJavaElement parent) {
		NestedClassElement clone = new NestedClassElement(getName(), isPublic());
		copyToClone(clone);
		return clone;
	}
	
	NestedClassElement copyIfDirty() {
		if (isDirty()) {
			NestedClassElement clone = new NestedClassElement(getName(), isPublic());
			copyIfDirty(clone);
			return clone;
		} 
		return null;
	}
}
