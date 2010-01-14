/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/threads/ThreadEffectsModule.java,v 1.2 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis.threads;

import com.surelogic.analysis.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public class ThreadEffectsModule extends AbstractWholeIRAnalysis<ThreadEffectsAnalysis,Void> {	
	public ThreadEffectsModule() {
		super("ThreadEffects");
	}

	public void init(IIRAnalysisEnvironment env) {
		// Nothing to do
	}
	
	@Override
	protected ThreadEffectsAnalysis constructIRAnalysis(IBinder binder) {
		return new ThreadEffectsAnalysis(binder);
	}
	
	@Override
	protected void clearCaches() {
		// Nothing to do
	}
	
	@Override
	protected boolean doAnalysisOnAFile(CUDrop cud, IRNode cu, IAnalysisMonitor monitor) {
		getAnalysis().analyzeCompilationUnit(cu, getResultDependUponDrop());
		return true;
	}
}
