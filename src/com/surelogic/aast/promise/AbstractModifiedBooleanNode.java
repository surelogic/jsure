package com.surelogic.aast.promise;

import edu.cmu.cs.fluid.java.JavaNode;

public abstract class AbstractModifiedBooleanNode extends AbstractBooleanNode {
	protected final int mods;
	
	protected AbstractModifiedBooleanNode(int offset, int modifiers) {
		super(offset);
		mods = modifiers;
	}

	public final boolean getModifier(int modifier) {
		return JavaNode.isSet(mods, modifier);
	}
	
	@Override
	protected String unparse(boolean debug, int indent, String token) {
		if (debug) {
			StringBuilder sb = new StringBuilder();
			if (debug) { indent(sb, indent); }
			sb.append(token);
			if (mods != JavaNode.ALL_FALSE) {
				if (JavaNode.isSet(mods, JavaNode.IMPLEMENTATION_ONLY)) {
					sb.append(" implementationOnly=true");
				}
				if (JavaNode.isSet(mods, JavaNode.NO_VERIFY)) {
					sb.append(" verify=false");
				}
			}
			sb.append('\n');
			return sb.toString();
		} else {
			return token;
		}
	}
	
	public final boolean isImplementationOnly() {
		return getModifier(JavaNode.IMPLEMENTATION_ONLY);
	}
	
	public final boolean verify() {
		return !getModifier(JavaNode.NO_VERIFY);
	}
}
