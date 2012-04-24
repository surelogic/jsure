package com.surelogic.xml;

import com.surelogic.common.CommonImages;

import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class FieldElement extends AnnotatedJavaElement implements IClassMember {
	FieldElement(String id, Access access) {
		super(id, access);
	}

	public String getLabel() {
		return "Field "+getName();
	}
	
	public final String getImageKey() {
		switch (getAccessibility()) {
		case PROTECTED:
			return CommonImages.IMG_PROTECTED_I;
		case DEFAULT:
			return CommonImages.IMG_DEFAULT_I;
		case PUBLIC:
		default:
			return CommonImages.IMG_PUBLIC_I;
		}
	}
	
	@Override
	public Operator getOperator() {
		return FieldDeclaration.prototype;
	}

	@Override
	FieldElement cloneMe(IJavaElement parent) {
		FieldElement clone = new FieldElement(getName(), getAccessibility());
		copyToClone(clone);
		return clone;
	}
	
	FieldElement copyIfDirty() {
		if (isDirty()) {
			FieldElement clone = new FieldElement(getName(), getAccessibility());
			copyIfDirty(clone);
			return clone;
		}
		return null;
	}
	
	public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
}
