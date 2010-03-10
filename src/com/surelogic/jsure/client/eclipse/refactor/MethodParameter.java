package com.surelogic.jsure.client.eclipse.refactor;

/**
 * MethodParameter represents a parameter on a method signature in a Java type.
 * 
 * @author nathan
 * 
 */
public class MethodParameter implements IJavaDeclaration {

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (method == null ? 0 : method.hashCode());
		result = prime * result + param;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final MethodParameter other = (MethodParameter) obj;
		if (method == null) {
			if (other.method != null) {
				return false;
			}
		} else if (!method.equals(other.method)) {
			return false;
		}
		if (param != other.param) {
			return false;
		}
		return true;
	}

}
