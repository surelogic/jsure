package com.surelogic.analysis.testing;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.JavaSemanticsVisitor;
import com.surelogic.analysis.Unused;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;

public final class BinderModule extends AbstractWholeIRAnalysis<IBinderClient, Unused> {
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
		runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			@Override
      public void run() {
				runOverFile(compUnit);
			}
		});
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
			super(true, false);
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
