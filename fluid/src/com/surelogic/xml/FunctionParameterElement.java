package com.surelogic.xml;

import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class FunctionParameterElement extends AnnotatedJavaElement {
	public static String PREFIX = "arg";
	private final int index;
	
	public FunctionParameterElement(boolean confirmed, int i) {
		super(confirmed, Integer.toString(i), Access.PUBLIC);
		index = i;
	}
	
	final int getIndex() {
		return index;
	}

	@Override
  public String getLabel() {
		final AbstractFunctionElement parent = (AbstractFunctionElement) getParent();
		try {
			if (parent == null) {
				return PREFIX+index;	
			}
			String[] params = parent.getSplitParams();
			if (params == null) {
				return PREFIX+index;
			}
			final String type = params[index];
			return PREFIX+index+" : "+type;
		} catch(IndexOutOfBoundsException e) {
			return PREFIX+index;			
		}
	}
	
	@Override
  public final String getImageKey() {
		return null; // TODO
	}
	
	@Override
	public Operator getOperator() {
		return ParameterDeclaration.prototype;
	}

	@Override
	FunctionParameterElement cloneMe(IJavaElement parent) {
		FunctionParameterElement clone = new FunctionParameterElement(isConfirmed(), index);
		copyToClone(clone);
		return clone;
	}
	
	FunctionParameterElement copyIfDirty() {
		if (isDirty()) {
			FunctionParameterElement clone = new FunctionParameterElement(isConfirmed(), index);
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
