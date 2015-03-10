/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.layers;

import com.surelogic.aast.promise.PromiseTargetNode;

public abstract class AbstractLayerMatchTarget extends PromiseTargetNode {
	AbstractLayerMatchTarget(int offset) {
		super(offset);
	}
	public abstract Iterable<String> getNames();
	
	public boolean matches(String qname) {
		for(String name : getNames()) {
			if (qname.equals(name)) {
				return true;
			}
		}
		return false;
	}
}
