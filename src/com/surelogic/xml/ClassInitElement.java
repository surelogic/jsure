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

	ClassInitElement merge(ClassInitElement clinit, MergeType type) {
		if (clinit != null) {
			mergeThis(clinit, type);
		}
		return this;
	}

	@Override
	ClassInitElement cloneMe() {
		ClassInitElement clone = new ClassInitElement();
		copyToClone(clone);
		return clone;
	}
}
