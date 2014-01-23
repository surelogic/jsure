package com.surelogic.analysis.testing;

import com.surelogic.analysis.*;
import com.surelogic.analysis.type.checker.ConstantExpressionVisitor;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AssignExpression;

public final class ConstantExpressionModule extends AbstractWholeIRAnalysis<IBinderClient, CUDrop> {
	public ConstantExpressionModule() {
		super("ConstantExpressionModule");
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
	  final ConstantExpressionVisitor ceVisitor;
	  
		public TestConstantExpressions() {
			super(true, false);
			ceVisitor = new ConstantExpressionVisitor(getBinder());
		}
		
		@Override
		public final Void visitAssignExpression(final IRNode e) {
		  super.visitAssignExpression(e);
		  
		  final IRNode rhs = AssignExpression.getOp2(e);
      final boolean isConstant = ceVisitor.doAccept(rhs);
      final HintDrop drop = HintDrop.newInformation(rhs);
      drop.setMessage(isConstant ? "Constant" : "Not Constant");
		  return null;
		}
	}
}
