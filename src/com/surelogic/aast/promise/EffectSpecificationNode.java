package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.parse.AbstractSingleNodeFactory;

import edu.cmu.cs.fluid.java.JavaNode;

public class EffectSpecificationNode extends AASTNode {
	// Fields
	private final boolean isWrite;
	private final ExpressionNode context;
	private final RegionSpecificationNode region;

	public static final AbstractSingleNodeFactory factory = new AbstractSingleNodeFactory(
			"EffectSpecification") {
		@Override
		@SuppressWarnings("unchecked")
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			boolean isWrite = JavaNode.getModifier(_mods, JavaNode.WRITE);
			ExpressionNode context = (ExpressionNode) _kids.get(0);
			RegionSpecificationNode region = (RegionSpecificationNode) _kids.get(1);
			return new EffectSpecificationNode(_start, isWrite, context, region);
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be
	 * 
	 * @unique
	 */
	public EffectSpecificationNode(int offset, boolean isWrite,
			ExpressionNode context, RegionSpecificationNode region) {
		super(offset);
		this.isWrite = isWrite;
		if (context == null) {
			throw new IllegalArgumentException("context is null");
		}
		((AASTNode) context).setParent(this);
		this.context = context;
		if (region == null) {
			throw new IllegalArgumentException("region is null");
		}
		((AASTNode) region).setParent(this);
		this.region = region;
	}

	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
			sb.append("EffectSpecification\n");
			indent(sb, indent + 2);
			sb.append("isWrite=").append(getIsWrite());
			sb.append("\n");
		}
		sb.append(getContext().unparse(debug, indent + 2));
		if (!debug) {
			sb.append(':');
		}
		sb.append(getRegion().unparse(debug, indent + 2));
		return sb.toString();
	}

	/**
	 * @return A non-null boolean
	 */
	public boolean getIsWrite() {
		return isWrite;
	}

	/**
	 * @return A non-null node
	 */
	public ExpressionNode getContext() {
		return context;
	}

	/**
	 * @return A non-null node
	 */
	public RegionSpecificationNode getRegion() {
		return region;
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {

		return visitor.visit(this);
	}

	@Override
	public IAASTNode cloneTree() {
		return new EffectSpecificationNode(getOffset(), getIsWrite(),
				(ExpressionNode) getContext().cloneTree(),
				(RegionSpecificationNode) getRegion().cloneTree());
	}
}