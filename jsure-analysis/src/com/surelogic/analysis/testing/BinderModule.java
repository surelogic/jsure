package com.surelogic.analysis.testing;

import com.surelogic.analysis.*;
import com.surelogic.analysis.visitors.JavaSemanticsVisitor;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;

public final class BinderModule extends AbstractWholeIRAnalysis<IBinderClient, CUDrop> {
	public BinderModule() {
		super("BinderModule");
	}

	@Override
	protected IBinderClient constructIRAnalysis(final IBinder binder) {
		return null;
	}

	@Override
	protected boolean doAnalysisOnAFile(
	    final IIRAnalysisEnvironment env, final CUDrop cud, final IRNode compUnit) {
		runOverFile(compUnit);
		return true;
	}

	protected void runOverFile(final IRNode compUnit) {
	  final TestConstantExpressions v = new TestConstantExpressions();
	  v.doAccept(compUnit);
	}	
	
	@Override
	protected void clearCaches() {
		// Nothing to do
	}
	
	private final class TestConstantExpressions extends JavaSemanticsVisitor {
		public TestConstantExpressions() {
			super(VisitInsideTypes.YES, SkipAnnotations.NO);
		}
		
		@Override
		protected final void handleMethodCall(final IRNode e) {
		  super.handleMethodCall(e);
		  processNode(e);
		}

		private void processNode(final IRNode e) {
		  final IRNode boundTo = getBinder().getBinding(e);
      final HintDrop drop = HintDrop.newInformation(e);
      drop.setMessage(DebugUnparser.toString(e) + " binds to "
          + JavaNames.genMethodConstructorName(boundTo));
		}
	}
}
