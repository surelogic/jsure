/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.java.adapter;

import edu.cmu.cs.fluid.java.JavaNode;

public class CodeContext {
	private final boolean isStatic;
	private final boolean fromInterface;

	public CodeContext(boolean isStatic, boolean fromInterface) {
		this.isStatic = isStatic;  	
		this.fromInterface = fromInterface;
	}

	public CodeContext(CodeContext context, int mods) {
		this(JavaNode.isSet(mods, JavaNode.STATIC), false);
	}

	public CodeContext(CodeContext context, boolean fromInterface) {
		this(context.isStatic, fromInterface);
	}

	public boolean isStatic() {
		return isStatic;
	}
	public boolean fromInterface() {
		return fromInterface;
	}
}
