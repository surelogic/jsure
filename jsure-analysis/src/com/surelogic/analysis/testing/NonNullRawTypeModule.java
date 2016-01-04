package com.surelogic.analysis.testing;

import com.surelogic.analysis.*;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis;
import com.surelogic.analysis.nullable.NonNullRawLattice.Element;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.Base;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.DebugQuery;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.Lattice;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.QualifiedThisQuery;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.Query;
import com.surelogic.analysis.visitors.AbstractJavaAnalysisDriver;
import com.surelogic.common.Pair;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.Triple;

public final class NonNullRawTypeModule extends AbstractWholeIRAnalysis<NonNullRawTypeAnalysis, CUDrop>{
  public NonNullRawTypeModule() {
    super("NonNull/Raw Types");
  }

  @Override
  protected NonNullRawTypeAnalysis constructIRAnalysis(final IBinder binder) {
    return new NonNullRawTypeAnalysis(binder);
  }

  @Override
  protected boolean doAnalysisOnAFile(final IIRAnalysisEnvironment env,
      final CUDrop cud, final IRNode compUnit) {
    runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
      @Override
      public void run() {
        checkRawTypesForFile(compUnit);
      }
    });
    return true;
  }

  protected void checkRawTypesForFile(final IRNode compUnit) {
    final RawTypeVisitor v = new RawTypeVisitor();
    v.doAccept(compUnit);
    getAnalysis().clear();
  }
  
  private final class RawTypeVisitor extends AbstractJavaAnalysisDriver<Triple<Query, QualifiedThisQuery, DebugQuery>> {
    public RawTypeVisitor() {
      super(true, false);
    }
    
    @Override
    protected Triple<Query, QualifiedThisQuery, DebugQuery> createNewQuery(final IRNode decl) {
      final NonNullRawTypeAnalysis analysis = getAnalysis();
      return new Triple<Query, QualifiedThisQuery, DebugQuery>(
          analysis.getRawTypeQuery(decl),
          analysis.getQualifiedThisQuery(decl),
          analysis.getDebugQuery(decl));
    }

    @Override
    protected Triple<Query, QualifiedThisQuery, DebugQuery> createSubQuery(final IRNode caller) {
      final Triple<Query, QualifiedThisQuery, DebugQuery> current = currentQuery();
      return new Triple<Query, QualifiedThisQuery, DebugQuery>(
          current.first().getSubAnalysisQuery(caller),
          current.second().getSubAnalysisQuery(caller),
          current.third().getSubAnalysisQuery(caller));
    }
    
    @Override
    public Void visitThisExpression(final IRNode expr) {
      // Ignore if ConstructorCall is the super expression
      final IRNode parent = JJNode.tree.getParent(expr);
      if (ConstructorCall.prototype.includes(parent) &&
          ConstructorCall.getObject(parent) == expr) {
        return null;
      }

      final IRNode rcvrDecl = JavaPromise.getReceiverNode(getEnclosingDecl());
      processReceiverDeclaration(expr, rcvrDecl);
      return null;
    }

    private void processReceiverDeclaration(
        final IRNode expr, final IRNode rcvrDecl) {
      final Pair<Lattice, Base[]> result =
          currentQuery().first().getResultFor(expr);
      final int idx = result.first().indexOf(rcvrDecl);
      final HintDrop drop = HintDrop.newInformation(expr);
      drop.setCategorizingMessage(Messages.DSC_NON_NULL);
      drop.setMessage(Messages.RAWNESS,  result.second()[idx]);
    }
    
    @Override
    public Void visitQualifiedThisExpression(final IRNode expr) {
      /* First check if we really aren't a fancy "this", that is,
       * C.this inside of class C.
       */
      // N.B. took this test from JavaEvaluationTransfer.transferUse()
      final IRNode decl = getBinder().getBinding(expr);
      if (ReceiverDeclaration.prototype.includes(decl)) {
        /*
         * The ReceiverDeclaration we get in 'decl' may be from an
         * InitDeclaration node, and not from the enclosing
         * ConsturctorDeclaration. All we want 'decl' for is to find out if the
         * QualifeidThisExpression really refers to a regular receiver. Now we
         * can get that receiver from the current enclosing declaration like we
         * do above in visitThisExpression().
         */
        processReceiverDeclaration(
            expr, JavaPromise.getReceiverNode(getEnclosingDecl()));
      } else {
        final Element v = currentQuery().second().getResultFor(expr);
        final HintDrop drop = HintDrop.newInformation(expr);
        drop.setCategorizingMessage(Messages.DSC_NON_NULL);
        drop.setMessage(Messages.QTHIS_RAWNESS, 
            QualifiedReceiverDeclaration.getJavaType(getBinder(), decl).toSourceText(),
            v);        
      }
      return null;
    }
    
    @Override
    public Void visitMethodBody(final IRNode b) {
      doAcceptForChildren(b);

      final String state = currentQuery().third().getResultFor(b);
      final HintDrop drop = HintDrop.newInformation(b);
      drop.setCategorizingMessage(Messages.DSC_NON_NULL);
      drop.setMessage(Messages.RAW_STATE, state);
      return null;
    }
    
    @Override
    public void handleConstructorCall(final IRNode expr) {
      final IRNode rcvrDecl = JavaPromise.getReceiverNode(getEnclosingDecl());
      processReceiverDeclaration(expr, rcvrDecl);
      
      super.handleConstructorCall(expr);
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
        final Pair<Lattice, Base[]> result =
            currentQuery().first().getResultFor(use);
        final int idx = result.first().indexOf(getBinder().getBinding(use));
        final Element state = result.second()[idx].first();
        
        final HintDrop drop = HintDrop.newInformation(use);
        drop.setCategorizingMessage(Messages.DSC_NON_NULL);
        final String varName = VariableUseExpression.getId(use);
        drop.setMessage(Messages.VAR_STATE, varName, state.toString());
      }
      
      return null;
    }
  }

  @Override
  protected void clearCaches() {
    // Nothing to do
  }
}
