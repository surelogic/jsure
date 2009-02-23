package edu.cmu.cs.fluid.analysis.usetypewherepossible;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.analysis.util.AbstractIRAnalysisModule;
import edu.cmu.cs.fluid.analysis.util.ConvertToIR;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.UseTypeWherePossibleAnalysis;

public class UseTypeWherePossibleGuidance extends AbstractIRAnalysisModule {
	/**
	 * Logger for this class
	 */
	@SuppressWarnings("unused")
	private static final Logger LOG = SLLogger
			.getLogger("UseTypeWherePossibleAssurance");

	static {
		ConvertToIR.prefetch("java.lang.Throwable");
	}

	UseTypeWherePossibleAnalysis useTypeWherePossibleAnalysis;

	@Override
	protected void constructIRAnalysis() {
		useTypeWherePossibleAnalysis = new UseTypeWherePossibleAnalysis(
				analysisContext);
	}

	@Override
	protected void doAnalysisOnAFile(IRNode cu) {
		useTypeWherePossibleAnalysis.analyzeCompilationUnit(cu);
	}
}
