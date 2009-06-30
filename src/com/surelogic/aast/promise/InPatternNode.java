/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/InPatternNode.java,v 1.4 2007/09/24 21:09:55 ethan Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Represents the root of an AAST that encapsulates a scoped promise's 'in'
 * portion
 * 
 * @author ethan
 */
public class InPatternNode extends AASTNode {
	private final InTypePatternNode typeNode;
	private final InPackagePatternNode packageNode;

	public static final AbstractSingleNodeFactory factory = new com.surelogic.parse.AbstractSingleNodeFactory(
			"InPattern") {

		@Override
		@SuppressWarnings("unchecked")
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {

			// FIXME This should change when we move away from supporting the
			// old-style
			InTypePatternNode typeNode = null;
			InPackagePatternNode packageNode = null;
			if (_kids.size() == 2) {
				typeNode = (InTypePatternNode) _kids.get(0);
				packageNode = (InPackagePatternNode) _kids.get(1);
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
			InPackagePatternNode packageNode) {
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
				sb.append(" in ");
				sb.append(getInTypePattern().unparse(debug));
				sb.append(getInPackagePattern().unparse(debug));
			}
		}
		return sb.toString();
	}

	public boolean matches(final IRNode decl) {
		boolean matches = true;
		if (typeNode != null && packageNode != null) {
			// System.out.println(unparse(false));
			if (packageNode.getInTypePattern() != null) {
				InPackagePatternNode ppn = packageNode.combineAASTs(typeNode);
				// System.out.println(ppn.unparse());
				matches = ppn.matches(decl);
			} else {
				matches = typeNode.matches(decl);
			}
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
	public InPackagePatternNode getInPackagePattern() {
		return packageNode;
	}

	@Override
	public IAASTNode cloneTree() {
		return new InPatternNode(getOffset(),
				(InTypePatternNode) getInTypePattern().cloneTree(),
				(InPackagePatternNode) getInPackagePattern().cloneTree());
	}
}
