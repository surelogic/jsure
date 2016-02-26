/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/WildcardTypeQualifierPatternNode.java,v 1.8 2008/10/01 20:56:15 chance Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * Represents a type qualifier string that can contain wildcard characters
 * 
 * @author ethan
 */
public class WildcardTypeQualifierPatternNode extends InTypePatternNode {
	private final String typePattern;

	public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
			"WildcardTypeQualifierPattern") {

		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			return new WildcardTypeQualifierPatternNode(_start, _id);
		}
	};

	public WildcardTypeQualifierPatternNode(int offset, String typePattern) {
		super(offset);
		this.typePattern = typePattern;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.promise.InTypePatternNode#matches(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	public boolean matches(IRNode irNode) {
		IRNode type = VisitUtil.getClosestType(irNode);
		if (type != null && TypeDeclaration.prototype.includes(type)) {
			if (typePattern.indexOf('*') < 0) {
				// No wildcards, so it can include a package name
				if (typePattern.indexOf('.') < 0) {
					// Single identifier, so match against type name
					return typePattern.matches(JavaNames.getTypeName(type));
				}
				return typePattern.matches(JavaNames.getFullTypeName(type));
			}
			return matches(JavaNames.getRelativeTypeName(type), typePattern);
		}
		return false;
	}

	/**
	 * Returns the qualified type pattern
	 * 
	 * @return
	 */
	public String getTypePattern() {
		return typePattern;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.AASTNode#accept(com.surelogic.aast.INodeVisitor)
	 */
	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.AASTNode#unparse(boolean, int)
	 */
	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
			sb.append("WildcardTypeQualifierPattern\n");
			indent(sb, indent + 2);
			sb.append("type=").append(getTypePattern());
			sb.append("\n");
		} else {
			sb.append(typePattern);
		}
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.IAASTNode#cloneTree()
	 */
	@Override
	protected IAASTNode internalClone(final INodeModifier mod) {
		return new WildcardTypeQualifierPatternNode(offset, typePattern);
	}
	
	@Override
	public boolean isFullWildcard() {
		return typePattern.length() == 0 || "*".equals(typePattern) || "**".equals(typePattern);
	}
}
