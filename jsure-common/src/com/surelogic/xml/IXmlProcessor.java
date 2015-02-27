package com.surelogic.xml;

public interface IXmlProcessor {
	void addPackage(String qname);
	void addType(String pkg, String name);
}
