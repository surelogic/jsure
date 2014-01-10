package com.surelogic.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;

/**
 * Represents a unit of analysis for IIRAnalysis
 */
public interface IAnalysisGranule {
	// Used to setup the assume context
	IRNode getCompUnit();

	ITypeEnvironment getTypeEnv();
	
	/**
	 * Used to identify the granule (esp. for debugging)
	 */
	String getLabel();
}
