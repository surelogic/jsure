/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast;

import java.util.*;

import com.surelogic.parse.*;

public abstract class AbstractAASTNodeFactory extends AbstractSingleNodeFactory<AASTNode> {
	public AbstractAASTNodeFactory(String token) {
		super(token);
	}

	@Override
	protected AASTNode createTempNode(List<AASTNode> kids) {
		return new TempListNode(kids);
	}
}
