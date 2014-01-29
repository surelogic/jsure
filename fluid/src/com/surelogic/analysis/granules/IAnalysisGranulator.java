package com.surelogic.analysis.granules;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import extra166y.Ops.Procedure;

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

	Class<T> getType();

	/**
	 * An opportunity to wrap the operation 
	 */
	// TODO setup versioning, assumptions, component locking 
	// TODO what if analysis starts another thread/task?
	// TODO unlock component cache
	Procedure<T> wrapAnalysis(Procedure<T> proc);
}
