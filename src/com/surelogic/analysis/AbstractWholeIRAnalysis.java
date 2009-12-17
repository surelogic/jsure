/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/AbstractWholeIRAnalysis.java,v 1.5 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.util.CachedSet;

public abstract class AbstractWholeIRAnalysis<T extends IBinderClient> extends AbstractIRAnalysis<T> {
	static private class ResultsDepDrop extends Drop {
		// Place holder class
	}
	
	/**
	 * Logger for this class
	 */
	protected final Logger LOG;
	
	private Drop resultDependUpon = null;
	
	protected AbstractWholeIRAnalysis(String logName) {
		LOG = SLLogger.getLogger(logName);
	}
	
	protected final Drop getResultDependUponDrop() {
		return resultDependUpon;
	}
	
	protected final void setResultDependUponDrop(IRReferenceDrop drop, IRNode node) {
		drop.setNode(node);
		setResultDependUponDrop(drop);
	}
	
	protected final void setResultDependUponDrop(IRReferenceDrop drop) {
		if (resultDependUpon != null && resultDependUpon.isValid()) {
			resultDependUpon.addDependent(drop);
		} else {
			LOG.log(Level.SEVERE,
					"setResultDependUponDrop found invalid or null resultDependUpon drop");
		}
	}
	
	protected final void setResultDependUponDrop(ResultDropBuilder drop, IRNode node) {
		drop.setNode(node);
		setResultDependUponDrop(drop);
	}
	
	protected final void setResultDependUponDrop(ResultDropBuilder p) {
		p.setResultDependUponDrop(resultDependUpon);
	}
	
	public boolean analyzeAll() {
		return true;
	}
	
	public void preAnalysis(IIRAnalysisEnvironment env, IIRProject p) {
		// Init the drop that all its results link to
	    if (resultDependUpon != null) {
	        resultDependUpon.invalidate();
	    }
    	resultDependUpon = new ResultsDepDrop();
	}
	
	public IRNode[] analyzeEnd(IIRProject p) {
		return JavaGlobals.noNodes;
	}
	
	public void postAnalysis(IIRProject p) {
		/*
		clearCaches();
		CachedSet.clearCache();
		*/
	}
	
	protected abstract void clearCaches();
}
