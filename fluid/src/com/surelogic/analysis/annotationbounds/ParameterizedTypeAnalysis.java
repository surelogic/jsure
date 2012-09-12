package com.surelogic.analysis.annotationbounds;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.Unused;
import com.surelogic.analysis.typeAnnos.AnnotationBoundsTypeFormalEnv;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public final class ParameterizedTypeAnalysis extends AbstractWholeIRAnalysis<GenericTypeInstantiationChecker, Unused> {
  public ParameterizedTypeAnalysis() {
		super("Parameterized Type");
	}

	@Override
	protected boolean flushAnalysis() {
		return true;
	}
	
	@Override
	protected void clearCaches() {
	  GenericTypeInstantiationChecker.clearCache();
	}
	
	@Override
	protected GenericTypeInstantiationChecker constructIRAnalysis(IBinder binder) {
		return new GenericTypeInstantiationChecker(
		    this, binder, AnnotationBoundsTypeFormalEnv.INSTANCE);
	}
	
	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode cu) {
    getAnalysis().doAccept(cu);
		return true;
	}
	
	@Override
	public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		finishBuild();
		return super.analyzeEnd(env, p);
	}
}
