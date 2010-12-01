/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.persistence;

import com.surelogic.aast.IAASTRootNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.*;

// TODO what result should this use? or store both?
// TODO where should this go?
public class AndAnalysisResult extends AbstractAnalysisResult {
	// TODO what about more complex expressions?
	private final PromiseRef[] dependencies;
	
	/**
	 * Result depends on all of the dependencies
	 */
	public <T extends IAASTRootNode> AndAnalysisResult(PromiseDrop<T> about, IRNode location, PromiseRef... deps) {
		super(about, location);
		dependencies = deps;
	}

	@Override
	protected void subEntitiesToXML(int indent, StringBuilder sb) {
		for(PromiseRef ref : dependencies) {
			ref.toXML(indent, sb, PersistenceConstants.AND_REF);
		}
	}
}
