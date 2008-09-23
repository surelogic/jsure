package com.surelogic.jsure.jdbc;

public enum DBType {

	// ORACLE("Oracle", "oracle"),
	DERBY("Apache Derby", "derby");

	private final String name;
	private final String prefix;

	DBType(String name, String prefix) {
		this.name = name;
		this.prefix = prefix;
	}

	public String getName() {
		return name;
	}

	public String getPrefix() {
		return prefix;
	}

}
