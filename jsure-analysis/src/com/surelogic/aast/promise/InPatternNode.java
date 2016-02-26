/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/InPatternNode.java,v 1.4 2007/09/24 21:09:55 ethan Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Represents the root of an AAST that encapsulates a scoped promise's 'in'
 * portion
 * 
 * @author ethan
 */
public class InPatternNode extends AASTNode {
	private final InTypePatternNode typeNode;
	private final InTypePatternNode packageNode;

	public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
			"InPattern") {

		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {

			// FIXME This should change when we move away from supporting the
			// old-style
			InTypePatternNode typeNode = null;
			InTypePatternNode packageNode = null;
			if (_kids.size() == 1) {
				packageNode = (InTypePatternNode) _kids.get(0);
			}
			else if (_kids.size() == 2) {
				typeNode = (InTypePatternNode) _kids.get(0);
				packageNode = (InTypePatternNode) _kids.get(1);
			}
			return new InPatternNode(_start, typeNode, packageNode);
		}
	};

	/**
	 * 
	 * @param offset
	 * @param typeNode
	 *          A possibly-null InTypePatternNode NOTE: This should change when we
	 *          move away from the old-style
	 * @param packageNode
	 *          A possibly-null InPackagePatternNod NOTE: This should change when
	 *          we move away from the old-style(
	 */
	public InPatternNode(int offset, InTypePatternNode typeNode,
			InTypePatternNode packageNode) {
		super(offset);
		this.typeNode = typeNode;
		this.packageNode = packageNode;
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
		if (typeNode != null) {
			if (debug) {
				indent(sb, indent);
				sb.append("InPattern\n");
				indent(sb, indent + 2);
				sb.append(getInTypePattern().unparse(debug, indent + 2));
				sb.append(getInPackagePattern().unparse(debug, indent + 2));
			} else {
				if (typeNode != null) {
					sb.append(" in ");
					sb.append(getInTypePattern().unparse(debug));
				}
				if (packageNode != null) {
					String unparse = getInPackagePattern().unparse(debug);
					if (unparse.length() > 0) {
						sb.append(" in ");
						sb.append(unparse);
					}
				}
			}
		}
		return sb.toString();
	}

	public boolean matches(final IRNode decl) {
		boolean matches = true;
		if (typeNode != null) {
			matches = typeNode.matches(decl);
		}
		if (packageNode != null) {
			matches = matches && packageNode.matches(decl);
		}
		return matches;
	}

	/**
	 * @return A possibly-null InTypePatternNode
	 */
	public InTypePatternNode getInTypePattern() {
		return typeNode;
	}

	/**
	 * @return A possibly-null InPackagePatternNode
	 */
	public InTypePatternNode getInPackagePattern() {
		return packageNode;
	}

	@Override
	protected IAASTNode internalClone(final INodeModifier mod) {
		return new InPatternNode(getOffset(),
				getInTypePattern() != null ? (InTypePatternNode) getInTypePattern().cloneOrModifyTree(mod) : null,
				getInPackagePattern() != null ? (InPackagePatternNode) getInPackagePattern().cloneOrModifyTree(mod) : null);
	}

	public boolean isFullWildcard() {
		return (typeNode == null || typeNode.isFullWildcard()) && 
		       (packageNode == null || packageNode.isFullWildcard());
	}
}
