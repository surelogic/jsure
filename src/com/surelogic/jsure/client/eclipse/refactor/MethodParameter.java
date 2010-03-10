package com.surelogic.jsure.client.eclipse.refactor;

/**
 * MethodParameter represents a parameter on a method signature in a Java type.
 * 
 * @author nathan
 * 
 */
public class MethodParameter {

	private final int param;
	private final Method method;

	MethodParameter(final Method method, final int param) {
		this.method = method;
		this.param = param;
	}

	/**
	 * The zero-based index of the parameter this object refers to.
	 * 
	 * @return
	 */
	public int getParam() {
		return param;
	}

	/**
	 * The method signature this parameter occurs in.
	 * 
	 * @return
	 */
	public Method getMethod() {
		return method;
	}

}
