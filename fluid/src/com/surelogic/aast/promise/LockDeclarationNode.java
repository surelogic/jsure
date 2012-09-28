package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class LockDeclarationNode extends AbstractLockDeclarationNode {
	// Fields
	private final RegionNameNode region;

	public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
			"LockDeclaration") {
		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			String id = _id;
			ExpressionNode field = (ExpressionNode) _kids.get(0);
			RegionNameNode region = (RegionNameNode) _kids.get(1);
			return new LockDeclarationNode(_start, id, field, region);
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be
	 * 
	 * @unique
	 */
	public LockDeclarationNode(int offset, String id, ExpressionNode field,
			RegionNameNode region) {
		super(offset, id, field);
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
			sb.append("LockDeclaration\n");
			indent(sb, indent + 2);
			sb.append("id=").append(getId());
			sb.append("\n");
			sb.append(getField().unparse(debug, indent + 2));
			sb.append(getRegion().unparse(debug, indent + 2));
		} else {
			sb.append("RegionLock(\"");
			sb.append(getId());
			sb.append(" is ");
			sb.append(getField().toString());
			sb.append(" protects ");
			sb.append(getRegion().toString());
			sb.append("\")");
		}
		return sb.toString();
	}

	/**
	 * @return A non-null node
	 */
	public RegionNameNode getRegion() {
		return region;
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {

		return visitor.visit(this);
	}

	@Override
	public IAASTNode cloneTree() {
		return new LockDeclarationNode(getOffset(), new String(getId()),
				(ExpressionNode) getField().cloneTree(),
				(RegionNameNode) getRegion().cloneTree());
	}
}
