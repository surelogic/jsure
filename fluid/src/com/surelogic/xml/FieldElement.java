package com.surelogic.xml;

import com.surelogic.common.CommonImages;

import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class FieldElement extends AnnotatedJavaElement implements IClassMember {
	FieldElement(boolean confirmed, String id, Access access) {
		super(confirmed, id, access);
	}

	@Override
  public String getLabel() {
		return "Field "+getName();
	}
	
	@Override
  public final String getImageKey() {
		switch (getAccessibility()) {
		case PROTECTED:
			return CommonImages.IMG_FIELD_PROTECTED;
		case DEFAULT:
			return CommonImages.IMG_FIELD_DEFAULT;
		case PUBLIC:
		default:
			return CommonImages.IMG_FIELD_PUBLIC;
		}
	}
	
	@Override
	public Operator getOperator() {
		return FieldDeclaration.prototype;
	}

	@Override
	FieldElement cloneMe(IJavaElement parent) {
		FieldElement clone = new FieldElement(isConfirmed(), getName(), getAccessibility());
		copyToClone(clone);
		return clone;
	}
	
	FieldElement copyIfDirty() {
		if (isDirty()) {
			FieldElement clone = new FieldElement(isConfirmed(), getName(), getAccessibility());
			copyIfDirty(clone);
			return clone;
		}
		return null;
	}
	
	@Override
  public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
}
