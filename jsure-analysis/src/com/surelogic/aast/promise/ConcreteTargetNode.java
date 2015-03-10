package com.surelogic.aast.promise;

public abstract class ConcreteTargetNode extends PromiseTargetNode {
	public ConcreteTargetNode(int offset) {
		super(offset);
	}

	public abstract boolean isFullWildcard();
}
