package com.surelogic.analysis;

import com.surelogic.analysis.granules.IAnalysisGranule;

public interface IAnalysisGroup<Q extends IAnalysisGranule> extends Iterable<IIRAnalysis<Q>> {
	int getOffset();
	boolean runSingleThreaded();
}
