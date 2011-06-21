package com.surelogic.xml;

import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class FunctionParameterElement extends AnnotatedJavaElement {
	private final int index;
	
	public FunctionParameterElement(int i) {
		super(Integer.toString(i));
		index = i;
	}
	
	final int getIndex() {
		return index;
	}

	public String getLabel() {
		return "Arg #"+index;
	}
	
	public final String getImageKey() {
		return null; // TODO
	}
	
	@Override
	public Operator getOperator() {
		return ParameterDeclaration.prototype;
	}

	FunctionParameterElement cloneMe() {
		FunctionParameterElement clone = new FunctionParameterElement(index);
		copyToClone(clone);
		return clone;
	}
}
