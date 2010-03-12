package com.surelogic.jsure.client.eclipse.refactor;

/**
 * A field declaration in some Java type.
 * 
 * @author nathan
 * 
 */
public class Field implements IJavaDeclaration {
	private final TypeContext type;
	private final String field;

	public Field(final TypeContext type, final String field) {
		this.type = type;
		this.field = field;
	}

	/**
	 * The name of the field.
	 * 
	 * @return
	 */
	public String getField() {
		return field;
	}

	public TypeContext getTypeContext() {
		return type;
	}

	public String forSyntax() {
		return String.format("%s in %s", field, type.forSyntax());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (field == null ? 0 : field.hashCode());
		result = prime * result + (type == null ? 0 : type.hashCode());
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
		final Field other = (Field) obj;
		if (field == null) {
			if (other.field != null) {
				return false;
			}
		} else if (!field.equals(other.field)) {
			return false;
		}
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		return true;
	}

}
