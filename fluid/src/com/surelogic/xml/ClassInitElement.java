package com.surelogic.xml;

import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public final class ClassInitElement extends AnnotatedJavaElement implements IClassMember {
	public ClassInitElement() {
		super("classinit");
	}

	public String getLabel() {
		return "<clinit>";
	}
	
	public final String getImageKey() {
		return null; // TODO
	}
	
	@Override
	public Operator getOperator() {
		return ClassInitDeclaration.prototype;
	}

	boolean merge(ClassInitElement clinit, MergeType type) {
		if (clinit != null) {
			return mergeThis(clinit, type);
		}
		return false;
	}

	@Override
	ClassInitElement cloneMe(IJavaElement parent) {
		ClassInitElement clone = new ClassInitElement();
		copyToClone(clone);
		return clone;
	}
	
	ClassInitElement copyIfDirty() {
		if (isDirty()) {
			ClassInitElement clone = new ClassInitElement();
			copyIfDirty(clone);
			return clone;
		}
		return null;
	}
	
	public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
}
