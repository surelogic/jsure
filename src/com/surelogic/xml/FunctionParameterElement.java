package com.surelogic.xml;

import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class FunctionParameterElement extends AnnotatedJavaElement {
	public static String PREFIX = "Arg #";
	private final int index;
	
	public FunctionParameterElement(int i) {
		super(Integer.toString(i));
		index = i;
	}
	
	final int getIndex() {
		return index;
	}

	public String getLabel() {
		AbstractFunctionElement parent = (AbstractFunctionElement) getParent();
		try {
			final String type = parent.getSplitParams()[index];
			return PREFIX+(index+1)+" : "+type;
		} catch(IndexOutOfBoundsException e) {
			return PREFIX+(index+1);
		}
	}
	
	public final String getImageKey() {
		return null; // TODO
	}
	
	@Override
	public Operator getOperator() {
		return ParameterDeclaration.prototype;
	}

	@Override
	FunctionParameterElement cloneMe() {
		FunctionParameterElement clone = new FunctionParameterElement(index);
		copyToClone(clone);
		return clone;
	}
	
	FunctionParameterElement copyIfDirty() {
		if (isDirty()) {
			FunctionParameterElement clone = new FunctionParameterElement(index);
			copyIfDirty(clone);
			return clone;
		}
		return null;
	}
	
	public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
}
