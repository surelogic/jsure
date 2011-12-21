package com.surelogic.jsure.client.eclipse.editors;

import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.tree.Operator;

public enum ScopedTargetType {
	METHOD("Methods", MethodDeclaration.prototype, "*(**)"),
	CONSTRUCTOR("Constructors", ConstructorDeclaration.prototype, "new(**)"),
	FUNC("Methods/Constructors", SomeFunctionDeclaration.prototype, "**(**)"),
	TYPE("Types", TypeDeclaration.prototype, "*"),
	FIELD("Fields", FieldDeclaration.prototype, "* *");
	
	final String label;
	final Operator op;
	final String target;
	
	private ScopedTargetType(String l, Operator o, String t) {
		label = l;
		op = o;
		target = t;
	}
}
