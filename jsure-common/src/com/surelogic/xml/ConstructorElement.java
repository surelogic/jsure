package com.surelogic.xml;

import com.surelogic.common.ref.IDecl;
import com.surelogic.common.xml.Entity;

//import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
//import edu.cmu.cs.fluid.tree.Operator;

public class ConstructorElement extends AbstractFunctionElement {
	public ConstructorElement(boolean confirmed, Access access, boolean isStatic, String params) {
		super(confirmed, "new", access, isStatic, params);
	}
	
	ConstructorElement(Entity e) {
		super("new", e);
	}

	public final IDecl.Kind getKind() {
		return IDecl.Kind.CONSTRUCTOR;
	}
	
	@Override
  public String getLabel() {
		return "Constructor("+getParams()+")";
	}
	
//	@Override
//	public Operator getOperator() {
//		return ConstructorDeclaration.prototype;
//	}

	@Override
	ConstructorElement cloneMe(IJavaElement parent) {
		ConstructorElement clone = new ConstructorElement(isConfirmed(), getAccessibility(), isStatic(), getParams());
		copyToClone(clone);
		return clone;
	}
	
	ConstructorElement copyIfDirty() {
		if (isDirty()) {
			ConstructorElement clone = new ConstructorElement(isConfirmed(), getAccessibility(), isStatic(), getParams());
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
