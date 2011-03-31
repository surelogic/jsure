package com.surelogic.aast.promise;

import com.surelogic.aast.*;

/**
 * Superclass for the variations of UniqueInRegion
 * 
 * @author Edwin
 */
public abstract class AbstractUniqueInRegionNode extends AASTRootNode {
	protected AbstractUniqueInRegionNode(int offset) {
		super(offset);
	}
	@Override
	public final boolean isHandledAsSuperclass() {
		return true;
	}	
	public abstract RegionSpecificationNode getSpec();
}
