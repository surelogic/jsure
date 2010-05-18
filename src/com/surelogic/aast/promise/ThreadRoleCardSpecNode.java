package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ThreadRoleCardSpecNode extends ThreadRoleAnnotationNode {
	// Fields
	private final ThreadRoleNameNode tRole;
	private final ThreadRoleCardChoiceNode card;

	public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
			"ThreadRoleCardinality") {
		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			ThreadRoleNameNode tRole = (ThreadRoleNameNode) _kids.get(0);
			ThreadRoleCardChoiceNode card = (ThreadRoleCardChoiceNode) _kids.get(1);
			return new ThreadRoleCardSpecNode(_start, tRole, card);
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be
	 * 
	 * @unique
	 */
	public ThreadRoleCardSpecNode(int offset, ThreadRoleNameNode tRole,
			ThreadRoleCardChoiceNode card) {
		super(offset);
		if (tRole == null) {
			throw new IllegalArgumentException("tRole is null");
		}
		((AASTNode) tRole).setParent(this);
		this.tRole = tRole;
		if (card == null) {
			throw new IllegalArgumentException("card is null");
		}
		((AASTNode) card).setParent(this);
		this.card = card;
	}

	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
		}
		sb.append("ThreadRoleCardinality\n");
		sb.append(getTRole().unparse(debug, indent + 2));
		sb.append(getCard().unparse(debug, indent + 2));
		return sb.toString();
	}

	/**
	 * @return A non-null node
	 */
	public ThreadRoleNameNode getTRole() {
		return tRole;
	}

	/**
	 * @return A non-null node
	 */
	public ThreadRoleCardChoiceNode getCard() {
		return card;
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
		return new ThreadRoleCardSpecNode(getOffset(), (ThreadRoleNameNode) getTRole()
				.cloneTree(), (ThreadRoleCardChoiceNode) getCard().cloneTree());
	}
}
