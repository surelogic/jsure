/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.layers;

import com.surelogic.aast.promise.PromiseTargetNode;

/**
 * Superclass for @Layer, @TypeSet
 * @author Edwin
 */
public abstract class AbstractLayerMatchDeclNode extends AbstractLayerMatchRootNode {
	private final String id;
	
	AbstractLayerMatchDeclNode(int offset, String id, PromiseTargetNode t) {
		super(offset, t);
		this.id = id;
	}

	public String getId() {
		return id;
	}	
}
