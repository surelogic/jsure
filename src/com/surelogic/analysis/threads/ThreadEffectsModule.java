/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/threads/ThreadEffectsModule.java,v 1.2 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis.threads;

import java.util.List;

import com.surelogic.analysis.*;
import com.surelogic.persistence.IAnalysisResult;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public class ThreadEffectsModule extends AbstractWholeIRAnalysis<ThreadEffectsAnalysis,Void> {	
	public ThreadEffectsModule() {
		super("ThreadEffects");
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
		List<IAnalysisResult> results = getAnalysis().analyzeCompilationUnit(cu, getResultDependUponDrop());		
		reportResults(cud, results);
		return true;
	}

	// TODO where should this really be?  superclass, or IIRAnalysisEnvironment?	
	private void reportResults(CUDrop cud, List<IAnalysisResult> results) {
		System.out.println("<compUnit path=\""+cud.javaOSFileName+"\">");
		for(IAnalysisResult r : results) {
			System.out.println(r.toXML(1));	
		}
		System.out.println("</compUnit>");
	}
}
