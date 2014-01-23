package com.surelogic.analysis;

import java.util.*;

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

	Class<T> getType();
}
