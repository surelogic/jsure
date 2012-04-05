package com.surelogic.xml;

public class NestedClassElement extends ClassElement implements IClassMember {
	public NestedClassElement(String id) {
		super(id);
	}

	@Override
	public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
	
	@Override
	NestedClassElement cloneMe(IJavaElement parent) {
		NestedClassElement clone = new NestedClassElement(getName());
		copyToClone(clone);
		return clone;
	}
	
	NestedClassElement copyIfDirty() {
		if (isDirty()) {
			NestedClassElement clone = new NestedClassElement(getName());
			copyIfDirty(clone);
			return clone;
		} 
		return null;
	}
}
