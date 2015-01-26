package com.surelogic.analysis.nullable;

import com.surelogic.analysis.*;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

public final class NullablePreprocessorModule extends AbstractWholeIRAnalysis<NullablePreprocessor, CUDrop> {
	public NullablePreprocessorModule() {
		super("NullablePreprocessorModule");
	}

	@Override
	protected NullablePreprocessor constructIRAnalysis(final IBinder binder) {
	  return new NullablePreprocessor(binder);
	}

	@Override
	protected boolean doAnalysisOnAFile(
	    final IIRAnalysisEnvironment env, final CUDrop cud, final IRNode compUnit) {
		runOverFile(compUnit);
		return true;
	}

	protected void runOverFile(final IRNode compUnit) {
	  getAnalysis().doAccept(compUnit);
	}	
	
	@Override
	protected void clearCaches() {
		// Nothing to do
	}
}
