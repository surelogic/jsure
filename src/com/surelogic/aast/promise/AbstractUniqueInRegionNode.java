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
	public abstract RegionSpecificationNode getSpec();
}
