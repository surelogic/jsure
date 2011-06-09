package com.surelogic.xml;

public class FunctionParameterElement extends AbstractJavaElement {
	private final int index;
	
	public FunctionParameterElement(int i) {
		super(Integer.toString(i));
		index = i;
	}
	
	final int getIndex() {
		return index;
	}

	@Override
	public String getLabel() {
		return "Arg #"+index;
	}
	
	@Override
	public final String getImageKey() {
		return null; // TODO
	}
}
