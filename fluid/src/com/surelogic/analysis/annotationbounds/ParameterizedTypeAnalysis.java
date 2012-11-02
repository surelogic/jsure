package com.surelogic.analysis.annotationbounds;

import java.util.Map;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.Unused;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.AnnotationBoundsPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;

public final class ParameterizedTypeAnalysis extends AbstractWholeIRAnalysis<GenericTypeInstantiationChecker, Unused> {
  private static final int NEVER_USED = 550;
  
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
		return new GenericTypeInstantiationChecker(this, binder);
	}
	
	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode cu) {
    getAnalysis().doAccept(cu);
		return true;
	}
	
	@Override
	public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
	  for (final IRNode unusedInstantiatedClass : GenericTypeInstantiationChecker.getUnusedBoundClasses()) {
	    final AnnotationBoundsPromiseDrop drop = LockRules.getAnnotationBounds(unusedInstantiatedClass);
	    ResultsBuilder.createResult(true, drop, unusedInstantiatedClass, NEVER_USED);
	  }
	  
		finishBuild();
		return super.analyzeEnd(env, p);
	}
	
	@Override
  public void finish(final IIRAnalysisEnvironment env) {
	  GenericTypeInstantiationChecker.clearStaticState();
	  super.finish(env);
  }
	
	
	
	public static Map<IJavaType, ResultFolderDrop> getFolders() {
	  return GenericTypeInstantiationChecker.getFolders();
	}
}
