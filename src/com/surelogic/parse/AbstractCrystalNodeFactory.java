/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.parse;

import java.util.List;

import com.surelogic.ast.java.operator.IJavaOperatorNode;

public abstract class AbstractCrystalNodeFactory 
extends AbstractSingleNodeFactory<IJavaOperatorNode> {
	public AbstractCrystalNodeFactory(String t) {
		super(t);
	}

	@Override
	protected IJavaOperatorNode createTempNode(List<IJavaOperatorNode> kids) {
		throw new UnsupportedOperationException();
	}	
}
