/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.java.adapter;

import edu.cmu.cs.fluid.java.JavaNode;

public class CodeContext {
	private final boolean isStatic;
	private final boolean fromInterface;
	private final boolean fromAnnotation;

	public CodeContext(boolean isStatic, boolean fromInterface, boolean fromAnnotation) {
		this.isStatic = isStatic;  	
		this.fromInterface = fromInterface;
		this.fromAnnotation = fromAnnotation;
	}

	public CodeContext(CodeContext context, int mods) {
		this(JavaNode.isSet(mods, JavaNode.STATIC), false, false);
	}

	public static CodeContext makeFromInterface(CodeContext context, boolean fromInterface) {
		return new CodeContext(context.isStatic, fromInterface, false);
	}
	
	public static CodeContext makeFromAnnotation(CodeContext context, boolean fromAnnotation) {
		return new CodeContext(context.isStatic, true, fromAnnotation);
	}

	public boolean isStatic() {
		return isStatic;
	}
	public boolean fromInterface() {
		return fromInterface;
	}
	public boolean fromAnnotation() {
		return fromAnnotation;
	}
}
