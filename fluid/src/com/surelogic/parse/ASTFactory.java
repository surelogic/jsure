/*$Header: /cvs/fluid/fluid/src/com/surelogic/parse/ASTFactory.java,v 1.14 2008/06/26 19:14:59 thallora Exp $*/
package com.surelogic.parse;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.promise.*;
import com.surelogic.annotation.parse.*;
import com.surelogic.common.logging.SLLogger;

public class ASTFactory extends AbstractASTFactory<AASTNode> {
	private ASTFactory() {
		// To make it private
	}

	private static final ASTFactory prototype = new ASTFactory();

	public static IASTFactory<AASTNode> getInstance() {
		return prototype;
	}

	static {
		final Logger log = SLLogger.getLogger();
		if (log.isLoggable(Level.FINE))
			log.fine("Initializing ASTFactory");
		FactoryRefs.register(prototype);
		MoreFactoryRefs.register(prototype);
		ThreadRoleFactoryRefs.register(prototype);
		LayerFactoryRefs.register(prototype);
	}

	@Override
	protected AASTNode createTempNode(List<AASTNode> kids) {
		return new TempListNode(kids);
	}
}
