package com.surelogic.aast.promise;

import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeModifier;
import com.surelogic.aast.INodeVisitor;

public class UtilityNode extends AbstractBooleanNode {
	public UtilityNode() {
		super();
	}
	
	@Override
	protected IAASTNode internalClone(final INodeModifier mod) {
		return new UtilityNode();
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String unparse(boolean debug, int indent) {
		return debug ? "UtilityNode" : "Utility";
	}
}
