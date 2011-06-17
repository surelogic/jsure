package com.surelogic.xml;

import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class ClassInitElement extends AnnotatedJavaElement implements IClassMember {
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
}
