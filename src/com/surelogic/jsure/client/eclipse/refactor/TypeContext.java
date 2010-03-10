package com.surelogic.jsure.client.eclipse.refactor;

/**
 * Represents a (possibly nested) type in a compilation unit
 * 
 * @author nathan
 * 
 */
public class TypeContext implements IJavaDeclaration {

	private final TypeContext parent;
	private final String name;
	private final Method method;

	TypeContext(final TypeContext parent, final String name) {
		if (parent == null) {
			throw new IllegalArgumentException(
					"If parent is null, use other constructor.");
		}
		this.method = null;
		this.name = name;
		this.parent = parent;
	}

	TypeContext(final String name) {
		this.method = null;
		this.parent = null;
		this.name = name;
	}

	public TypeContext(final Method m, final String id) {
		this.parent = m.getType();
		this.method = m;
		this.name = id;
	}

	public TypeContext getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	public Method getMethod() {
		return method;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (method == null ? 0 : method.hashCode());
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + (parent == null ? 0 : parent.hashCode());
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
		final TypeContext other = (TypeContext) obj;
		if (method == null) {
			if (other.method != null) {
				return false;
			}
		} else if (!method.equals(other.method)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (parent == null) {
			if (other.parent != null) {
				return false;
			}
		} else if (!parent.equals(other.parent)) {
			return false;
		}
		return true;
	}

}
