package com.surelogic.xml;

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
}
