package com.surelogic.xml;

public class NestedClassElement extends ClassElement implements IClassMember {
	public NestedClassElement(boolean confirmed, String id, Access access) {
		super(confirmed, id, access);
	}

	@Override
	public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
	
	@Override
	NestedClassElement cloneMe(IJavaElement parent) {
		NestedClassElement clone = new NestedClassElement(isConfirmed(), getName(), getAccessibility());
		copyToClone(clone);
		return clone;
	}
	
	@Override
  NestedClassElement copyIfDirty() {
		if (isDirty()) {
			NestedClassElement clone = new NestedClassElement(isConfirmed(), getName(), getAccessibility());
			copyIfDirty(clone);
			return clone;
		} 
		return null;
	}
}
