package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.java.TypeNode;
import com.surelogic.parse.AbstractSingleNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.*;

public class FieldDeclPatternNode extends PromiseTargetNode {
	// Fields
	private final int mods;
	private final TypeNode ftype;
	private final TypeQualifierPatternNode type;
	private final String name;
	private final InPatternNode inPattern;

	public static final AbstractSingleNodeFactory factory = new AbstractSingleNodeFactory(
			"FieldDeclPattern") {
		@Override
		@SuppressWarnings("unchecked")
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			int mods = _mods;
			TypeNode ftype = (TypeNode) _kids.get(0);
			TypeQualifierPatternNode type = (TypeQualifierPatternNode) _kids.get(1);
			InPatternNode inPattern = (InPatternNode) _kids.get(2);
			String name = _id;
			return new FieldDeclPatternNode(_start, mods, ftype, type, inPattern,
					name);
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be
	 * 
	 * @unique
	 */
	public FieldDeclPatternNode(int offset, int mods, TypeNode ftype,
			TypeQualifierPatternNode type, InPatternNode inPattern, String name) {
		super(offset);
		this.mods = mods;
		if (ftype == null) {
			throw new IllegalArgumentException("ftype is null");
		}
		((AASTNode) ftype).setParent(this);
		this.ftype = ftype;
		if (type == null) {
			throw new IllegalArgumentException("type is null");
		}
		((AASTNode) type).setParent(this);
		this.type = type;
		if (name == null) {
			throw new IllegalArgumentException("name is null");
		}
		this.name = name;

		if (inPattern == null) {
			throw new IllegalArgumentException("inPattern is null");
		}
		this.inPattern = inPattern;
	}

	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
			sb.append("FieldDeclPattern\n");
			indent(sb, indent + 2);
			sb.append("mods=").append(getMods());
			sb.append("\n");
			sb.append(getFtype().unparse(debug, indent + 2));
			sb.append(getType().unparse(debug, indent + 2));
			indent(sb, indent + 2);
			sb.append("name=").append(getName());
			sb.append("\n");
			sb.append(getInPattern().unparse(debug, indent + 2));
		} else {
			sb.append(getFtype().unparse(debug)).append(' ');
			sb.append(getType().unparse(debug)).append('.');
			sb.append(getName());
			sb.append(getInPattern().unparse(debug));
		}
		return sb.toString();
	}

	/**
	 * @return A non-null int
	 */
	public int getMods() {
		return mods;
	}

	/**
	 * @return A non-null node
	 */
	public TypeNode getFtype() {
		return ftype;
	}

	/**
	 * @return A non-null node
	 */
	public TypeQualifierPatternNode getType() {
		return type;
	}

	/**
	 * @return A non-null String
	 */
	public String getName() {
		return name;
	}

	/*****************************************************************************
	 * @return A non-null InPatternNode
	 */
	public InPatternNode getInPattern() {
		return inPattern;
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {

		return visitor.visit(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.promise.PromiseTargetNode#matches(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	public boolean matches(IRNode irNode) {
		if (FieldDeclaration.prototype.includes(irNode)) {
			IRNode vdecl = VariableDeclarators.getVar(FieldDeclaration
					.getVars(irNode), 0);

			boolean ret = matchesModifiers(mods, FieldDeclaration.getMods(irNode));
			ret = ret && matchesName(VariableDeclarator.getId(vdecl));
			ret = ret && type.matches(irNode, true);
			ret = ret && ftype.matches(FieldDeclaration.getType(irNode));
			ret = ret && inPattern.matches(irNode);
			return ret;
		}
		return false;
	}

	/**
	 * @param irNode
	 * @return
	 */
	private boolean matchesName(final String dName) {
		if (name.indexOf("*") < 0) {
			// straight-up name comparison
			return name.equals(dName);
		}
		// replace all ** and * with .*
		final String pattern = name.replaceAll("\\*+", ".*");
		return dName.matches(pattern);
	}

	@Override
	public IAASTNode cloneTree() {
		return new FieldDeclPatternNode(getOffset(), getMods(),
				(TypeNode) getFtype().cloneTree(), (TypeQualifierPatternNode) getType()
						.cloneTree(), (InPatternNode) getInPattern().cloneTree(),
				new String(getName()));
	}
}