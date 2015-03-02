package com.surelogic.jsure.client.eclipse.editors;

import com.surelogic.common.ref.IDecl.Kind;

public enum ScopedTargetType {
	METHOD("Methods", Kind.METHOD, "*(**)"),
	CONSTRUCTOR("Constructors", Kind.CONSTRUCTOR, "new(**)"),
	//FUNC("Methods/Constructors", null, "**(**)"),
	TYPE("Types", Kind.CLASS, "*"),
	FIELD("Fields", Kind.FIELD, "* *");
	
	final String label;
	final Kind kind;
	final String target;
	
	private ScopedTargetType(String l, Kind o, String t) {
		label = l;
		kind = o;
		target = t;
	}
}
