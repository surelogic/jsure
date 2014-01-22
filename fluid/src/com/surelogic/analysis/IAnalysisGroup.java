package com.surelogic.analysis;

public interface IAnalysisGroup<Q extends IAnalysisGranule> extends Iterable<IIRAnalysis<Q>> {
	int getOffset();
	boolean runSingleThreaded();
}
