package com.surelogic.jsure.client.eclipse.refactor;

/**
 * Represents a (possibly nested) type in a compilation unit
 * 
 * @author nathan
 * 
 */
public class Type {

	private final Type parent;
	private final String name;

	Type(final Type parent, final String name) {
		if (parent == null) {
			throw new IllegalArgumentException(
					"If parent is null, use other constructor.");
		}
		this.parent = parent;
		this.name = name;
	}

	Type(final String name) {
		this.parent = null;
		this.name = name;
	}

	public Type getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		final Type other = (Type) obj;
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
