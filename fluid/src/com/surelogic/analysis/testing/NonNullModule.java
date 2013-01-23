package com.surelogic.analysis.testing;

import java.util.Set;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.Unused;
import com.surelogic.analysis.nullable.NonNullAnalysis;
import com.surelogic.analysis.nullable.NonNullAnalysis.Query;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.parse.JJNode;

public final class NonNullModule extends AbstractWholeIRAnalysis<NonNullAnalysis, Unused>{
  public NonNullModule() {
    super("Non Null");
  }

  @Override
  protected NonNullAnalysis constructIRAnalysis(final IBinder binder) {
    return new NonNullAnalysis(binder);
  }

  @Override
  protected boolean doAnalysisOnAFile(final IIRAnalysisEnvironment env,
      final CUDrop cud, final IRNode compUnit) {
    runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
      @Override
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
  
  private final class NonNullVisitor extends AbstractJavaAnalysisDriver<Query> {
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
      // Ignore if we are the LHS of an assignment
      final IRNode parent = JJNode.tree.getParent(use);
      if (AssignExpression.prototype.includes(parent) &&
          AssignExpression.getOp1(parent).equals(use)) {
        return null;
      }
      
      // See if the current variable is a primitive or not
      final IJavaType type = getBinder().getJavaType(use);
      if (type instanceof IJavaReferenceType) {
         // See if the current variable is considered to be null or not
        final Set<IRNode> nonNull = currentQuery().getResultFor(use);
        final IRNode varDecl = getBinder().getBinding(use);
        final HintDrop drop = HintDrop.newInformation(use);
        drop.setCategorizingMessage(Messages.DSC_NON_NULL);
        final String varName = VariableUseExpression.getId(use);
        if (nonNull.contains(varDecl)) {
          drop.setMessage(Messages.NOT_NULL, varName);
        } else {
          drop.setMessage(Messages.MAYBE_NULL, varName);
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
