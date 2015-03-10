package com.surelogic.analysis.testing;

import com.surelogic.analysis.*;
import com.surelogic.analysis.assigned.DefiniteAssignment;
import com.surelogic.analysis.assigned.DefiniteAssignment.ProvablyUnassignedQuery;
import com.surelogic.analysis.granules.FlowUnitGranulator;
import com.surelogic.analysis.granules.FlowUnitGranule;
import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.analysis.visitors.FlowUnitVisitor;
import com.surelogic.dropsea.ir.HintDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.util.TypeUtil;

public final class FinalModule extends AbstractWholeIRAnalysis<DefiniteAssignment, FlowUnitGranule> {
	public FinalModule() {
		super("FinalModule");
	}

	@Override
	protected DefiniteAssignment constructIRAnalysis(final IBinder binder) {
		return new DefiniteAssignment(binder);
	}
	
	@Override
	protected void clearCaches() {
		// Nothing to do
	}

  @Override
  public IAnalysisGranulator<FlowUnitGranule> getGranulator() {
    return FlowUnitGranulator.prototype;
  }
	
  @Override
  protected boolean doAnalysisOnGranule_wrapped(
      final IIRAnalysisEnvironment env, final FlowUnitGranule g) {
    g.execute(new TestFinalDeclarations(getBinder(), getAnalysis()));
    return true;
  }
	
	
	private final class TestFinalDeclarations extends FlowUnitVisitor<ProvablyUnassignedQuery> {
    final IBinder binder;
    final DefiniteAssignment defAssign;
    
    public TestFinalDeclarations(final IBinder b, final DefiniteAssignment da) {
      super(true);
      binder = b;
      defAssign = da;
    }
    
    @Override
    protected ProvablyUnassignedQuery createNewQuery(final IRNode decl) {
      return defAssign.getProvablyUnassignedQuery(decl);
    }

    @Override
    protected ProvablyUnassignedQuery createSubQuery(final IRNode caller) {
      return currentQuery().getSubAnalysisQuery(caller);
    }

    
    
		private void testDeclaration(final IRNode decl, final boolean testEffectivelyFinal) {
      final StringBuilder msg = new StringBuilder();
		  final boolean isJavaFinal = TypeUtil.isJavaFinal(decl);
      msg.append("isJavaFinal = ");
      msg.append(isJavaFinal);
		  final boolean isJSureFinal = TypeUtil.isJSureFinal(decl);
      msg.append("; isJSureFinal = ");
      msg.append(isJSureFinal);
      if (!isJSureFinal && testEffectivelyFinal) {
        final boolean isEffectivelyFinal = TypeUtil.isEffectivelyFinal(decl, binder, currentQuery());
        msg.append("; isEffectivelyFinal = ");
        msg.append(isEffectivelyFinal);
      }
		  final HintDrop drop = HintDrop.newInformation(decl);
		  drop.setMessage(msg.toString());
		}
		
		private void dumpDefiniteAssignment(final IRNode node) {
      final HintDrop drop = HintDrop.newInformation(node);
      drop.setMessage(currentQuery().getResultFor(node).toString());
		}
		
		@Override
		public final void handleLocalVariableDeclaration(final IRNode e) {
		  testDeclaration(e, true);
		  super.handleLocalVariableDeclaration(e);
		}
		
		@Override
		public final void handleFieldInitialization(IRNode varDecl, boolean isStatic) {
		  testDeclaration(varDecl, false);
		  super.handleFieldInitialization(varDecl, isStatic);
		}
		
		@Override
		public final Void visitParameterDeclaration(IRNode node) {
		  testDeclaration(node, true);
		  return null;
		}
		
		@Override
		public final Void visitStatement(final IRNode stmt) {
		  dumpDefiniteAssignment(stmt);
		  super.visitStatement(stmt);
		  return null;
		}
    
    @Override
    public final Void visitExpression(final IRNode expr) {
      dumpDefiniteAssignment(expr);
      super.visitExpression(expr);
      return null;
    }
	}
}
