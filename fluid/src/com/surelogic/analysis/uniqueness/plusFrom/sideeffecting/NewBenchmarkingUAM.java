package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting;

import java.util.Collections;

import com.surelogic.analysis.*;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
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
    public ConcurrencyType runInParallel() {
    	return ConcurrencyType.NEVER;
    }

    @Override
    protected void startAnalyzeBegin(IIRProject p, IBinder binder) {
      super.startAnalyzeBegin(p, binder);
    }

	@Override
	protected UniquenessAnalysis constructIRAnalysis(IBinder binder) {
	  final boolean shouldTimeOut = IDE.getInstance().getBooleanPreference(
	      IDEPreferences.TIMEOUT_FLAG);
	  return new UniquenessAnalysis(binder, shouldTimeOut);
	}
	
	@Override
	protected void clearCaches() {
		getAnalysis().clearCaches();
	}
	
	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode compUnit) {
		for (final IRNode node : JJNode.tree.topDown(compUnit)) {
			final Operator op = JJNode.tree.getOperator(node);
			if (MethodDeclaration.prototype.includes(op) || ConstructorDeclaration.prototype.includes(op)) {
				String methodName = JavaNames.genQualifiedMethodConstructorName(node);
				if (env.getMonitor() != null) {
					env.getMonitor().subTask("Checking [ Uniqueness Assurance ] " + methodName);
				}
				methodName = methodName.replace(',', '_');

				JavaComponentFactory.clearCache();
				final ISrcRef srcRef = JavaNode.getSrcRef(node);
				final int length = srcRef == null ? -1 : srcRef.getLength();
				final long start = System.currentTimeMillis();
				String msg;
				try {
					final int numLocals =  getAnalysis().getAnalysis(node).getLattice().getNumLocals();
					final long end = System.currentTimeMillis();
					msg = methodName + ", " + length + ", " + numLocals + ", " + (end-start);
				} catch(final AnalysisGaveUp e) {
					msg = methodName + ", " + length + ", TIMEOUT after " + e.count + " worklist steps";
				}
				System.out.print(msg);
				System.out.println(ImmutableHashOrderSet.clearCaches());				
			}
	  }
	  return false;
	}

	@Override
	public Iterable<IRNode>  analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
    // Create the drops from the drop builders
    finishBuild();
		
		// FIX only clearing some of the threads?
		if (getAnalysis() != null) {
			getAnalysis().clear();
		}
		return Collections.emptyList();
	}
}
