package com.surelogic.jsure.client.eclipse.refactor;

/**
 * A method signature in a Java type.
 * 
 * @author nathan
 * 
 */
public class Method {
	private final Type type;
	private final String method;
	private final String[] params;

	/**
	 * Construct a new method with the given type and param classesn
	 * 
	 * @param type
	 * @param params
	 *            the simple names of each parameter class, or an empty array if
	 *            there are no parameters
	 */
	Method(final Type type, final String method, final String[] params) {
		this.type = type;
		this.method = method;
		this.params = params;
	}

	/**
	 * The type this method signature is declared in.
	 * 
	 * @return
	 */
	public Type getType() {
		return type;
	}

	/**
	 * The name of the method
	 * 
	 * @return
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * An array of the parameter types, not including {@code this}
	 * 
	 * @return
	 */
	public String[] getParams() {
		return params;
	}

}
