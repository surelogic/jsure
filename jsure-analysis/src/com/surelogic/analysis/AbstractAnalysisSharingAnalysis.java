package com.surelogic.analysis;

import com.surelogic.analysis.granules.IAnalysisGranule;

import edu.cmu.cs.fluid.java.bind.IBinder;

public abstract class AbstractAnalysisSharingAnalysis<S, T extends IBinderClient, Q extends IAnalysisGranule> extends AbstractWholeIRAnalysis<T, Q> {
	protected final SharedAnalysisFactory<S> factory;
	private S sharedAnalysis;
	
	protected AbstractAnalysisSharingAnalysis(boolean inParallel, Class<Q> type, String logName, SharedAnalysisFactory<S> f) {
		super(inParallel, type, logName);
		factory = f;
	}

	@Override
	public Class<S> getGroup() {
		return factory.getGroup();
	}
	
	@Override
	protected void startAnalyzeBegin(IIRProject p, IBinder binder) {
		super.startAnalyzeBegin(p, binder);
		sharedAnalysis = factory.get(binder);		
	}
	
	protected final S getSharedAnalysis() {
		return sharedAnalysis;
	}
	
	@Override
	protected void clearCaches() {
		sharedAnalysis = null;
		factory.clear();
	}
}