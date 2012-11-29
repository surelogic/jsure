package com.surelogic.aast.promise;

import com.surelogic.Part;
import com.surelogic.aast.java.NamedTypeNode;

import edu.cmu.cs.fluid.java.JavaNode;

public abstract class AbstractModifiedBooleanNode extends AbstractBooleanNode {	
	public static final String APPLIES_TO = "appliesTo";	
	public static enum State {
		Immutable, ThreadSafe, NotThreadSafe
	}
	
	protected final String token;
	protected final int mods;
	protected final Part appliesTo;
	
	protected AbstractModifiedBooleanNode(String tkn, int modifiers, Part state) {
		super();
		token = tkn;
		mods = modifiers;
		appliesTo = state != null ? state : Part.InstanceAndStatic;
	}

	public final String getToken() {
	  return token;
	}
	
	public final boolean getModifier(int modifier) {
		return JavaNode.isSet(mods, modifier);
	}
  
  @Override
  public final String unparse(boolean debug, int indent) {
    return unparse(debug, indent, token);
  }
	
	@Override
	protected final String unparse(boolean debug, int indent, String token) {
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
				if (JavaNode.isSet(mods, JavaNode.ALLOW_REF_OBJECT)) {
					sb.append(" allowReferenceObject=true");
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
			if (JavaNode.isSet(mods, JavaNode.ALLOW_REF_OBJECT)) {
				if (!first) {
					sb.append(", ");
				}
				sb.append("allowReferenceObject=true");
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
	
	protected void unparseExtra(boolean debug, int indent, StringBuilder sb) {
		if (appliesTo != Part.InstanceAndStatic) {
			if (debug) {
				sb.append(' ');
			} 
			sb.append(APPLIES_TO).append('=').append(appliesTo);			
		}
	}

	/**
	 * @return true if unparsed something
	 */
	protected final boolean unparseTypes(boolean debug, int indent, StringBuilder sb, String kind, NamedTypeNode[] types) {
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
	
	protected boolean hasChildren() {
		return appliesTo != Part.InstanceAndStatic;
	}
	
	public final boolean isImplementationOnly() {
		return getModifier(JavaNode.IMPLEMENTATION_ONLY);
	}
	
	public final boolean verify() {
		return !getModifier(JavaNode.NO_VERIFY);
	}
	
	public final boolean allowReferenceObject() {
		return getModifier(JavaNode.ALLOW_REF_OBJECT);
	}
	
	public final Part getAppliesTo() {
		return appliesTo;
	}
}
