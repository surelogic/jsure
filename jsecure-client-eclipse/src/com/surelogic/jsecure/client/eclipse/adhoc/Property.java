package com.surelogic.jsecure.client.eclipse.adhoc;

public class Property {
	final String expr;
	String label;
	
	Property(String e) {
		expr = e;
		label = e;
	}
	
	void setLabel(String l) {
		label = l;
	}
}
