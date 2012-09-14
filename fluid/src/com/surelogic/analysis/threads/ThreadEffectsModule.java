/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/threads/ThreadEffectsModule.java,v 1.2 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis.threads;

import java.io.*;
import java.util.List;

import com.surelogic.analysis.*;
import com.surelogic.persistence.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public class ThreadEffectsModule extends AbstractWholeIRAnalysis<ThreadEffectsAnalysis,Unused> {	
	public ThreadEffectsModule() {
		super("ThreadEffects");
	}
	
	@Override
	protected ThreadEffectsAnalysis constructIRAnalysis(IBinder binder) {
		return new ThreadEffectsAnalysis(this, binder);
	}
	
	@Override
	protected void clearCaches() {
		// Nothing to do
	}
	
	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode cu) {
		List<IAnalysisResult> results = getAnalysis().analyzeCompilationUnit(cu);	
		if (results.isEmpty()) {
			return true;
		}
		try {
			try {
				OutputStream out = env.makeResultStream(cud);
				if (out != null) {
					JSureResultsXMLCreator c = new JSureResultsXMLCreator(out);
					c.reportResults(cud, results);
				}
			} finally {
				env.closeResultStream();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	@Override
	public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		finishBuild();
		return super.analyzeEnd(env, p);
	}
}
