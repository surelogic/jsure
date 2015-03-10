package com.surelogic.analysis;

import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.analysis.granules.IAnalysisGranule;

public interface IAnalysisGroup<Q extends IAnalysisGranule> extends Iterable<IIRAnalysis<Q>> {
	int getOffset();
	boolean runSingleThreaded();
	IAnalysisGranulator<Q> getGranulator();
	Class<Q> getGranuleType();
	Analyses getParent();
	int size();
}
