package com.surelogic.analysis.granules;

import java.util.List;

import com.surelogic.common.concurrent.Procedure;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;

/**
 * Extracts one or more IAnalysisGranules from a compilation unit
 * 
 * @author Edwin
 */
public interface IAnalysisGranulator<T extends IAnalysisGranule> {
	/**
	 * @param tEnv 
	 * @return the number of granules added
	 */	
	int extractGranules(ITypeEnvironment tEnv, IRNode cu);
	
	/**
	 * Destructively reads the granules collected
	 */
	List<T> getGranules();

	// Returned immediately
	List<T> extractNewGranules(ITypeEnvironment tEnv, IRNode cu);
	
	Class<T> getType();

	/**
	 * If there are N or above, fork off tasks
	 * Otherwise, run sequentially
	 */
	int getThresholdToForkTasks();
	
	/**
	 * An opportunity to wrap the operation 
	 */
	// TODO setup versioning, assumptions, component locking 
	// TODO what if analysis starts another thread/task?
	// TODO unlock component cache
	Procedure<T> wrapAnalysis(Procedure<T> proc);
}
