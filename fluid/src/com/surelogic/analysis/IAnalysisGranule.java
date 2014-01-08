package com.surelogic.analysis;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Represents a unit of analysis for IIRAnalysis
 */
public interface IAnalysisGranule {
	// Used to setup the assume context
	IRNode getCompUnit();
}
