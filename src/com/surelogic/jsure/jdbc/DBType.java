package com.surelogic.jsure.jdbc;

import com.surelogic.common.jdbc.IDBType;

public enum DBType implements IDBType {

	//ORACLE("Oracle", "oracle"), 
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
