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

}
