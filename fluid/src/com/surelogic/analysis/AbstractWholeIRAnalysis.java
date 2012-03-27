/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/AbstractWholeIRAnalysis.java,v 1.5 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.control.LabelList;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.proxy.*;
import edu.cmu.cs.fluid.util.CachedSet;

public abstract class AbstractWholeIRAnalysis<T extends IBinderClient, Q extends ICompUnitContext> extends AbstractIRAnalysis<T,Q> {
	public static final boolean useDependencies = System.getProperty("SureLogic.useDependencies", "false").equals("true");
	public static final boolean debugDependencies = useDependencies && false;
	
	static private class ResultsDepDrop extends Drop {
		final Class<?> clazz;
		
		// Place holder class
		ResultsDepDrop(Class<?> clazz) {
			this.clazz = clazz;
		}
	}
	
	/**
	 * Logger for this class
	 */
	protected final Logger LOG;
	
	private Drop resultDependUpon = findResultsDepDrop();
	
	protected AbstractWholeIRAnalysis(String logName) {
		this(false, null, logName);
	}
	
	protected AbstractWholeIRAnalysis(boolean inParallel, Class<Q> type, String logName) {
		super(inParallel, type);
		LOG = SLLogger.getLogger(logName);
	}
	
	private Drop findResultsDepDrop() {	    
		for(ResultsDepDrop d : Sea.getDefault().getDropsOfExactType(ResultsDepDrop.class)) {
			if (d.clazz == this.getClass()) {
				//System.out.println("Found old ResultsDepDrop for "+this);
				return d;
			}
		}        
		return null;
	}
	
	protected final Drop getResultDependUponDrop() {
		return resultDependUpon;
	}
	
	public final void setResultDependUponDrop(IRReferenceDrop drop, IRNode node) {
		drop.setNodeAndCompilationUnitDependency(node);
		setResultDependUponDrop(drop);		
	}
	
	private final void setResultDependUponDrop(IRReferenceDrop drop) {
		if (useDependencies) {
			return;
		}
		if (resultDependUpon != null && resultDependUpon.isValid()) {
			resultDependUpon.addDependent(drop);
		} else {
			LOG.log(Level.SEVERE,
					"setResultDependUponDrop found invalid or null resultDependUpon drop");
		}
	}
	
	public final void setResultDependUponDrop(AbstractDropBuilder drop, IRNode node) {
		if (useDependencies) {
			drop.setNodeAndCompilationUnitDependency(node);
		} else {
			drop.setNode(node);
			setResultDependUponDrop(drop);
		}		
	}
	
	private final void setResultDependUponDrop(AbstractDropBuilder p) {
		if (!useDependencies) {
			p.addDependUponDrop(resultDependUpon);
		} else {
			System.out.println("No depend upon drop");
		}
	}
	
	public final boolean analyzeAll() {
		return true;
	}
	
	@Override
	public void init(IIRAnalysisEnvironment env) {
		if (useDependencies) {
			return;
		}
		// Init the drop that all its results link to
		if (resultDependUpon != null) {
	    	resultDependUpon.invalidate();
	    }
    	resultDependUpon = new ResultsDepDrop(this.getClass());
	}
	
	@Override
	public void postAnalysis(IIRProject p) {
		clearCaches();
		CachedSet.clearCache();
		LabelList.clearCache();
	}
	
	protected abstract void clearCaches();
}
