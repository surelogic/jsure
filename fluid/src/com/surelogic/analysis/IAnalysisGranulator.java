package com.surelogic.analysis;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Extracts one or more IAnalysisGranules from a compilation unit
 * 
 * @author Edwin
 */
public interface IAnalysisGranulator<T extends IAnalysisGranule> {
	/**
	 * @return the number of granules added
	 */	
	int extractGranules(List<T> granules, IRNode cu);
}
