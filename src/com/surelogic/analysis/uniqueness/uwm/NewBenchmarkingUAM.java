package com.surelogic.analysis.uniqueness.uwm;

import com.surelogic.analysis.*;
import com.surelogic.analysis.effects.Effects;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.uwm.cs.fluid.control.FlowAnalysis.AnalysisGaveUp;

public class NewBenchmarkingUAM extends AbstractWholeIRAnalysis<UniquenessAnalysis,Void> {
  public NewBenchmarkingUAM() {
		super(false, null, "UniqueAnalysis (NEW)");
	}

	@Override
	protected void startAnalyzeBegin(IIRProject p, IBinder binder) {
		// Nothing to do
	}

	@Override
	protected UniquenessAnalysis constructIRAnalysis(IBinder binder) {
	  return new UniquenessAnalysis(binder, new Effects(binder));
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
				if (monitor != null) {
					monitor.subTask("Checking [ Uniqueness Assurance ] " + methodName);
				}
				methodName = methodName.replace(',', '_');

				JavaComponentFactory.clearCache();
				final ISrcRef srcRef = JavaNode.getSrcRef(node);
				final int length = srcRef == null ? -1 : srcRef.getLength();
				final long start = System.currentTimeMillis();
				String msg;
				try {
					getAnalysis().getAnalysis(node);
					final long end = System.currentTimeMillis();
					msg = methodName + ", " + length + ", " + (end-start);
				} catch(final AnalysisGaveUp e) {
					msg = methodName + ", " + length + ", GAVE UP AFTER ~2 MINUTES: " + e.count + " STEPS";
				}
				System.out.print(msg);
				System.out.println(ImmutableHashOrderSet.clearCaches());				
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
