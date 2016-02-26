package com.surelogic.aast;

/**
 * 
 * 
 * @author edwin
 */
public interface INodeModifier {
	enum Status {
		KEEP, CLONE, MODIFY
	}
	
	/**
	 * Check this particular AAST node to see if we need to clone/modify it
	 */
	Status createNewAAST(IAASTNode n); 
	
	/**
	 * Create the modified AAST node
	 */
	IAASTNode modify(IAASTNode orig);
		
	static final INodeModifier CLONE = new INodeModifier() {
		@Override
		public Status createNewAAST(IAASTNode n) {
			return Status.CLONE;
		}

		@Override
		public IAASTNode modify(IAASTNode orig) {
			throw new IllegalStateException("Should never get here");
		}		
	};
}
