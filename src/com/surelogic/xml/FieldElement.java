package com.surelogic.xml;

import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class FieldElement extends AnnotatedJavaElement implements IClassMember {
	FieldElement(String id) {
		super(id);
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
	FieldElement cloneMe() {
		FieldElement clone = new FieldElement(getName());
		copyToClone(clone);
		return clone;
	}
	
	public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
}
