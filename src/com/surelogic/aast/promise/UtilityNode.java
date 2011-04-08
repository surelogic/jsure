package com.surelogic.aast.promise;

import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;

public class UtilityNode extends AbstractBooleanNode {
	public UtilityNode(int offset) {
		super(offset);
	}
	
	@Override
	public IAASTNode cloneTree() {
		return new UtilityNode(offset);
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
