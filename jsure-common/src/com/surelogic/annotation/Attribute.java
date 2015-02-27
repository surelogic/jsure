package com.surelogic.annotation;

import com.surelogic.common.i18n.I18N;

public final class Attribute implements Comparable<Attribute> {
	private final String f_name;
	private final Class<?> f_type;
	/**
	 * May be null.
	 */
	private final String f_defaultValue;

	public String getName() {
		return f_name;
	}

	public Class<?> getType() {
		return f_type;
	}
	
	public boolean isTypeString() {
		return String.class.equals(f_type);
	}

	public String getDefaultValueOrNull() {
		return f_defaultValue;
	}

	public Attribute(String name, Class<?> type, String defaultValue) {
		if (name == null)
			throw new IllegalArgumentException(I18N.err(44, "name"));
		if (type == null)
			throw new IllegalArgumentException(I18N.err(44, "type"));

		f_name = name;
		f_type = type;
		f_defaultValue = defaultValue;
	}

	@Override
public int compareTo(Attribute o) {
		return f_name.compareTo(o.f_name);
	}

	@Override
	public String toString() {
		return "[Attribute " + f_name + " : " + f_type.getName() + " def "
				+ f_defaultValue + "]";
	}
}
