package com.surelogic.analysis.testing;

import com.surelogic.analysis.*;
import com.surelogic.analysis.bca.BindingContext;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.bca.BindingContextAnalysis.Query;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.util.ImmutableSet;

public final class BCAModule extends AbstractWholeIRAnalysis<BindingContextAnalysis, Unused> {
	public BCAModule() {
		super("BCACategory");
	}

	@Override
	protected BindingContextAnalysis constructIRAnalysis(final IBinder binder) {
	  return new BindingContextAnalysis(binder, false, true);
	}
  
  @Override
  protected void clearCaches() {
    // Nothing to do
  }

	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode compUnit) {
		runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			public void run() {
				runOverFile(compUnit);
			}
		});
		return true;
	}

	protected void runOverFile(final IRNode compUnit) {
    final BCAVisitor v = new BCAVisitor();
    v.doAccept(compUnit);
    getAnalysis().clear();
	}	
	
  private final class BCAVisitor extends AbstractJavaAnalysisDriver<BindingContextAnalysis.Query> {
    @Override
    protected Query createNewQuery(final IRNode decl) {
      return getAnalysis().getExpressionObjectsQuery(decl);
    }

    @Override
    protected Query createSubQuery(final IRNode caller) {
      return currentQuery().getSubAnalysisQuery(caller);
    }
    
    

    @Override
    protected void enteringEnclosingType(final IRNode newType) {
      System.out.println(">>> Entering type " + JavaNames.getTypeName(newType));
    }
    
    @Override
    protected void leavingEnclosingType(final IRNode newType) {
      System.out.println("<<< Leaving type " + JavaNames.getTypeName(newType));
    }
    
    @Override
    protected void enteringEnclosingDeclPrefix(
        final IRNode newDecl, final IRNode anonClassDecl) {
      final String name = JavaNames.genQualifiedMethodConstructorName(newDecl);
      System.out.println("Running BCA on " + name);
    }
    
    
    
    @Override
    public Void visitVariableUseExpression(final IRNode use) {
      // See if the current variable is a primitive or not
      final IJavaType type = getBinder().getJavaType(use);
      if (type instanceof IJavaReferenceType) {
        final ImmutableSet<IRNode> bindings = currentQuery().getResultFor(use);
        final HintDrop drop = HintDrop.newInformation(use);
        drop.setCategorizingString(Messages.DSC_BCA);
        drop.setMessage(Messages.BINDS_TO,
            VariableUseExpression.getId(use),
            BindingContext.setToString(bindings));
      }
      
      return null;
    }
  }
}
