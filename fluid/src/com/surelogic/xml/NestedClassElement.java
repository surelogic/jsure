package com.surelogic.xml;

public class NestedClassElement extends ClassElement implements IClassMember {
	NestedClassElement(String id) {
		super(id);
	}

	NestedClassElement cloneMe() {
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
