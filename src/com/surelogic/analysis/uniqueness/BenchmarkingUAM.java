package com.surelogic.analysis.uniqueness;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.effects.Effects;

import edu.cmu.cs.fluid.control.FlowAnalysis.AnalysisGaveUp;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

public class BenchmarkingUAM extends AbstractWholeIRAnalysis<UniqueAnalysis,Void> {
  public BenchmarkingUAM() {
		super(false, null, "UniqueAnalysis");
	}
  
	public void init(IIRAnalysisEnvironment env) {
		// Nothing to do
	}

	@Override
	protected void startAnalyzeBegin(IIRProject p, IBinder binder) {
		// Nothing to do
	}

	@Override
	protected UniqueAnalysis constructIRAnalysis(IBinder binder) {
		return new UniqueAnalysis(binder,	new Effects(binder), 5000);
	}
	
	@Override
	protected void clearCaches() {
		getAnalysis().clearCaches();
	}
	
	@Override
	protected boolean doAnalysisOnAFile(CUDrop cud, IRNode compUnit, IAnalysisMonitor monitor) {
	  for (final IRNode node : JJNode.tree.topDown(compUnit)) {
	    final Operator op = JJNode.tree.getOperator(node);
	    if (MethodDeclaration.prototype.includes(op) || ConstructorDeclaration.prototype.includes(op)) {
        String methodName = JavaNames.genQualifiedMethodConstructorName(node);
        monitor.subTask("Checking [ Uniqueness Assurance ] " + methodName);
        methodName = methodName.replace(',', '_');
        
        JavaComponentFactory.clearCache();
        final ISrcRef srcRef = JavaNode.getSrcRef(node);
        final int length = srcRef == null ? -1 : srcRef.getLength();
        final long start = System.currentTimeMillis();
        try {
          getAnalysis().getAnalysis(node);
          final long end = System.currentTimeMillis();
          System.out.println(
              methodName + ", " + length + ", " + (end-start));
        } catch(final AnalysisGaveUp e) {
          System.out.println(
              methodName + ", " + length + ", GAVE UP AFTER " + e.count + " STEPS");
        }
        ImmutableHashOrderSet.clearCaches();
	    }
	  }
	  return false;
	}

	@Override
	public IRNode[] analyzeEnd(IIRProject p) {
    // Create the drops from the drop builders
    finishBuild();
		
		// FIX only clearing some of the threads?
		if (getAnalysis() != null) {
			getAnalysis().clear();
		}
		return JavaGlobals.noNodes;
	}
}
