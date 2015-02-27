package com.surelogic.xml;

import com.surelogic.common.ref.IDecl;

//import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
//import edu.cmu.cs.fluid.tree.Operator;

public final class ClassInitElement extends AnnotatedJavaElement implements IClassMember {
	public ClassInitElement() {
		super(false, "classinit", Access.DEFAULT);
	}

	public final IDecl.Kind getKind() {
		return IDecl.Kind.INITIALIZER; // TODO is this right?
	}
	
	@Override
  public String getLabel() {
		return "<clinit>";
	}
	
	@Override
  public final String getImageKey() {
		return null; // TODO
	}
	
//	@Override
//	public Operator getOperator() {
//		return ClassInitDeclaration.prototype;
//	}

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
	
	@Override
  public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
}
