package com.surelogic.aast.promise;

import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;

public class SingletonNode extends AbstractBooleanNode {
	public SingletonNode() {
		super();
	}
	
	@Override
	public IAASTNode cloneTree() {
		return new SingletonNode();
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String unparse(boolean debug, int indent) {
		return debug ? "SingletonNode" : "Singleton";
	}
}
