package com.surelogic.analysis.testing;

import java.util.Set;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IIRAnalysisEnvironment;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis.Query;

public final class NonNullModule extends AbstractWholeIRAnalysis<SimpleNonnullAnalysis, Void>{
  public NonNullModule() {
    super("Non Null");
  }

  @Override
  protected SimpleNonnullAnalysis constructIRAnalysis(final IBinder binder) {
    return new SimpleNonnullAnalysis(binder);
  }

  @Override
  protected boolean doAnalysisOnAFile(final IIRAnalysisEnvironment env,
      final CUDrop cud, final IRNode compUnit) {
    runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
      public void run() {
        checkNonNullForFile(compUnit);
      }
    });
    return true;
  }

  protected void checkNonNullForFile(final IRNode compUnit) {
    final NonNullVisitor v = new NonNullVisitor();
    v.doAccept(compUnit);
    getAnalysis().clear();
//    JavaComponentFactory.clearCache();
  }
  
  private final class NonNullVisitor extends AbstractJavaAnalysisDriver<SimpleNonnullAnalysis.Query> {
    @Override
    protected Query createNewQuery(final IRNode decl) {
      return getAnalysis().getNonnullBeforeQuery(decl);
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
      System.out.println("############################ Running non null on " + JavaNames.genQualifiedMethodConstructorName(newDecl) + "############################");
    }
    
    
    
    @Override
    public Void visitVariableUseExpression(final IRNode use) {
      // See if the current variable is a primitive or not
      final IJavaType type = getBinder().getJavaType(use);
      if (type instanceof IJavaReferenceType) {
         // See if the current variable is considered to be null or not
        final Set<IRNode> nonNull = currentQuery().getResultFor(use);
        final IRNode varDecl = getBinder().getBinding(use);
        final InfoDrop drop = new InfoDrop(null);
        setResultDependUponDrop(drop, use);
        drop.setCategory(Messages.DSC_NON_NULL);
        final String varName = VariableUseExpression.getId(use);
        if (nonNull.contains(varDecl)) {
          drop.setResultMessage(Messages.NOT_NULL, varName);
        } else {
          drop.setResultMessage(Messages.MAYBE_NULL, varName);
        }
      }
      
      return null;
    }
  }

  @Override
  protected void clearCaches() {
    // Nothing to do
  }
}
