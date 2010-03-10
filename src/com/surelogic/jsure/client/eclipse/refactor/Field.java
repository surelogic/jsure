package com.surelogic.jsure.client.eclipse.refactor;

/**
 * A field declaration in some Java type.
 * 
 * @author nathan
 * 
 */
public class Field {
	private final Type type;
	private final String field;

	public Field(final Type type, final String field) {
		this.type = type;
		this.field = field;
	}

	/**
	 * The type this field is declared in.
	 * 
	 * @return
	 */
	public Type getType() {
		return type;
	}

	/**
	 * The name of the field.
	 * 
	 * @return
	 */
	public String getField() {
		return field;
	}

}
