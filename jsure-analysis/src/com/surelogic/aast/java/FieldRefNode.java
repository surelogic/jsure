// Generated code.  Do *NOT* edit!
package com.surelogic.aast.java;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class FieldRefNode extends PrimaryExpressionNode implements
		IHasVariableBinding {
	// Fields
	private final ExpressionNode object;
	private final String id;

	public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
			"FieldRef") {
		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			ExpressionNode object = (ExpressionNode) _kids.get(0);
			String id = _id;
			return new FieldRefNode(_start, object, id);
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be
	 * 
	 * @unique
	 */
	public FieldRefNode(int offset, ExpressionNode object, String id) {
		super(offset);
		if (object == null) {
			throw new IllegalArgumentException("object is null");
		}
		((AASTNode) object).setParent(this);
		this.object = object;
		if (id == null) {
			throw new IllegalArgumentException("id is null");
		}
		this.id = id;
	}

	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
			sb.append("FieldRef\n");
			sb.append(getObject().unparse(debug, indent + 2));
			indent(sb, indent + 2);
			sb.append("id=").append(getId());
			sb.append("\n");
		} else {
			sb.append(getObject().toString());
			sb.append('.');
			sb.append(getId());
		}
		return sb.toString();
	}

	@Override
  public boolean bindingExists() {
		return AASTBinder.getInstance().isResolvable(this);
	}

	@Override
  public IVariableBinding resolveBinding() {
		return AASTBinder.getInstance().resolve(this);
	}

	/**
	 * @return A non-null node
	 */
	public ExpressionNode getObject() {
		return object;
	}

	/**
	 * @return A non-null String
	 */
	public String getId() {
		return id;
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new FieldRefNode(getOffset(), (ExpressionNode) getObject()
				.cloneTree(), new String(getId()));
	}
}
