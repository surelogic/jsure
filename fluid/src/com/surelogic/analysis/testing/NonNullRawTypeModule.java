package com.surelogic.analysis.testing;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.Unused;
import com.surelogic.analysis.nullable.combined.NonNullRawLattice.Element;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.DebugQuery;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.Lattice;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.QualifiedThisQuery;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.Query;
import com.surelogic.common.Pair;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.Triple;

public final class NonNullRawTypeModule extends AbstractWholeIRAnalysis<NonNullRawTypeAnalysis, Unused>{
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
      final Pair<Lattice, Element[]> result =
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

      /* This is no good for regression tests because the order of elements
       * in the set isn't fixed.  Not worth fixing them.  Any errors here
       * will be reflected as errors in qualified this expressions.
       */
//      final Pair<Lattice, Element[]> pair = currentQuery().first().getResultFor(b);
//      final HintDrop drop2 = HintDrop.newInformation(b);
//      drop2.setCategorizingMessage(Messages.DSC_NON_NULL);
//      drop2.setMessage(Messages.USES, pair.first().qualifiedThisToString());

      return null;
    }
    
    @Override
    public void handleConstructorCall(final IRNode expr) {
      final IRNode rcvrDecl = JavaPromise.getReceiverNode(getEnclosingDecl());
      processReceiverDeclaration(expr, rcvrDecl);
      
      super.handleConstructorCall(expr);
    }
  }

  @Override
  protected void clearCaches() {
    // Nothing to do
  }
}
