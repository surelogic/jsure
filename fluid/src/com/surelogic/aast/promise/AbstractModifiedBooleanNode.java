package com.surelogic.aast.promise;

import com.surelogic.aast.java.NamedTypeNode;

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
			unparseExtra(debug, indent, sb);
			sb.append('\n');
			return sb.toString();
		} else if (mods != JavaNode.ALL_FALSE || hasChildren()) {
			StringBuilder sb = new StringBuilder(token);
			boolean first = true;
			sb.append('(');			
			if (JavaNode.isSet(mods, JavaNode.IMPLEMENTATION_ONLY)) {
				sb.append("implementationOnly=true");
				first = false;
			}
			if (JavaNode.isSet(mods, JavaNode.NO_VERIFY)) {
				if (!first) {
					sb.append(", ");
				}
				sb.append("verify=false");
			}
			if (hasChildren()) {
				if (!first) {
					sb.append(", ");
				}
				unparseExtra(debug, indent, sb);
			}
			sb.append(')');
			return sb.toString();
		} else {	 
			return token;
		}
	}	
	
	protected abstract void unparseExtra(boolean debug, int indent, StringBuilder sb);

	/**
	 * @return true if unparsed something
	 */
	protected boolean unparseTypes(boolean debug, int indent, StringBuilder sb, String kind, NamedTypeNode[] types) {
		if (types.length == 0) {
			return false;
		}
		if (debug) {
			sb.append('\n');
			indent(sb, indent);
			sb.append(' ');
		}
		sb.append(kind).append('=');
		boolean first = true;
		for(NamedTypeNode t : types) {
			if (first) {
				first = false;
			} else {
				sb.append(',');
			}
			sb.append(t.unparse(debug, indent));
		}
		return true;
	}
	
	protected abstract boolean hasChildren();
	
	public final boolean isImplementationOnly() {
		return getModifier(JavaNode.IMPLEMENTATION_ONLY);
	}
	
	public final boolean verify() {
		return !getModifier(JavaNode.NO_VERIFY);
	}
	
	public static interface WhenVisitor {
	  public void visitWhenType(NamedTypeNode namedType);
	}
	
	public abstract void visitAnnotationBounds(WhenVisitor visitor);
}
