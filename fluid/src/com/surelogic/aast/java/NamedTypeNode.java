package com.surelogic.aast.java;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.ISourceRefType;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.*;

public class NamedTypeNode extends ClassTypeNode {
	// Fields
	private final String type;

	public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
			"NamedType") {
		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			String type = _id;
			return new NamedTypeNode(_start, type);
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be
	 * 
	 * @unique
	 */
	public NamedTypeNode(int offset, String type) {
		super(offset);
		if (type == null) {
			throw new IllegalArgumentException("type is null");
		}
		this.type = type;
	}

	@Override
	public String unparse(boolean debug, int indent) {
		if (!debug) {
			return getType();
		}
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
		}
		sb.append("NamedType\n");
		indent(sb, indent + 2);
		sb.append("type=").append(getType());
		sb.append("\n");
		return sb.toString();
	}

	@Override
	public boolean typeExists() {
		return AASTBinder.getInstance().isResolvableToType(this);
	}

	/**
	 * Gets the binding corresponding to the type of the NamedType
	 */
	@Override
	public ISourceRefType resolveType() {
		return AASTBinder.getInstance().resolveType(this);
	}

	/**
	 * @return A non-null String
	 */
	public String getType() {
		return type;
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {

		return visitor.visit(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.java.TypeNode#matches(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	public boolean matches(IRNode type) {
		if ("*".equals(getType())) {
			return true;
		}
		if(NameType.prototype.includes(type)){
			type = NameType.getName(type);
		}
		
		final String dName;
		if (NamedType.prototype.includes(type)) {
			dName = NamedType.getType(type);
		} else if (SimpleName.prototype.includes(type)) {
			dName = SimpleName.getId(type);
		} else if (QualifiedName.prototype.includes(type)) {
			dName = QualifiedName.getId(type);
		} else {
    	    //System.out.println("NamedType. No match with: " + JJNode.tree.getOperator(type).getClass());
			return false;
		}
		if (getType().indexOf("*") < 0) {
			// No wildcards
			return getType().equals(dName); // FIXME inner classes
		}
		final String pattern = getType().replaceAll("\\*", ".*");
		return dName.matches(pattern);
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new NamedTypeNode(getOffset(), new String(getType()));
	}
}
