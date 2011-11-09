package com.surelogic.jsure.client.eclipse.editors;

import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.tree.Operator;

public enum ScopedTargetType {
	METHOD("methods", MethodDeclaration.prototype, "*(**)"),
	CONSTRUCTOR("constructors", ConstructorDeclaration.prototype, "new(**)"),
	FUNC("methods/constructors", SomeFunctionDeclaration.prototype, "**(**)"),
	TYPE("types", TypeDeclaration.prototype, "*"),
	FIELD("fields", FieldDeclaration.prototype, "* *");
	
	final String label;
	final Operator op;
	final String target;
	
	private ScopedTargetType(String l, Operator o, String t) {
		label = l;
		op = o;
		target = t;
	}
}
