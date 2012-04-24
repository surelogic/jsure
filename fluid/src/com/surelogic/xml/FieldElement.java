package com.surelogic.xml;

import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class FieldElement extends AnnotatedJavaElement implements IClassMember {
	FieldElement(String id, boolean isPublic) {
		super(id, isPublic);
	}

	public String getLabel() {
		return "Field "+getName();
	}
	
	public final String getImageKey() {
		return null; // TODO
	}
	
	@Override
	public Operator getOperator() {
		return FieldDeclaration.prototype;
	}

	@Override
	FieldElement cloneMe(IJavaElement parent) {
		FieldElement clone = new FieldElement(getName(), isPublic());
		copyToClone(clone);
		return clone;
	}
	
	FieldElement copyIfDirty() {
		if (isDirty()) {
			FieldElement clone = new FieldElement(getName(), isPublic());
			copyIfDirty(clone);
			return clone;
		}
		return null;
	}
	
	public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
}
