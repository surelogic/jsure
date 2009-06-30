/*$Header: /cvs/fluid/fluid/src/com/surelogic/parse/ASTFactory.java,v 1.14 2008/06/26 19:14:59 thallora Exp $*/
package com.surelogic.parse;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.aast.promise.*;
import com.surelogic.annotation.parse.ColorFactoryRefs;
import com.surelogic.annotation.parse.FactoryRefs;
import com.surelogic.annotation.parse.MoreFactoryRefs;
import com.surelogic.common.logging.SLLogger;

public class ASTFactory extends AbstractASTFactory {
	private ASTFactory() {
		// To make it private
	}

	private static final ASTFactory prototype = new ASTFactory();

	public static IASTFactory getInstance() {
		return prototype;
	}

	static {
		final Logger log = SLLogger.getLogger();
		if (log.isLoggable(Level.FINE))
			log.fine("Initializing ASTFactory");
		FactoryRefs.register(prototype);
		MoreFactoryRefs.register(prototype);
		ColorFactoryRefs.register(prototype);

		// These two lines are added so that opgen doesn't have to be modified
		// for these exceptions
		ReadsNode.factory.register(prototype);
		WritesNode.factory.register(prototype);
		RegionEffectsNode.factory.register(prototype);
		FieldMappingsNode.factory.register(prototype);
	}
}
