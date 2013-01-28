package com.surelogic.analysis.testing;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.Unused;
import com.surelogic.analysis.nullable.RawTypeAnalysis;
import com.surelogic.analysis.nullable.RawTypeAnalysis.DebugQuery;
import com.surelogic.analysis.nullable.RawTypeAnalysis.Inferred;
import com.surelogic.analysis.nullable.RawTypeAnalysis.InferredRawQuery;
import com.surelogic.analysis.nullable.RawTypeAnalysis.Lattice;
import com.surelogic.analysis.nullable.RawTypeAnalysis.Query;
import com.surelogic.analysis.nullable.RawLattice.Element;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.common.Pair;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.nullable.RawPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.Triple;

public final class RawTypeModule extends AbstractWholeIRAnalysis<RawTypeAnalysis, Unused>{
  public RawTypeModule() {
    super("Raw Types");
  }

  @Override
  protected RawTypeAnalysis constructIRAnalysis(final IBinder binder) {
    return new RawTypeAnalysis(binder);
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
  
  private final class RawTypeVisitor extends AbstractJavaAnalysisDriver<Triple<Query, InferredRawQuery, DebugQuery>> {
    @Override
    protected Triple<Query, InferredRawQuery, DebugQuery> createNewQuery(final IRNode decl) {
      final RawTypeAnalysis analysis = getAnalysis();
      return new Triple<Query, InferredRawQuery, DebugQuery>(
          analysis.getRawTypeQuery(decl),
          analysis.getInferredRawQuery(decl),
          analysis.getDebugQuery(decl));
    }

    @Override
    protected Triple<Query, InferredRawQuery, DebugQuery> createSubQuery(final IRNode caller) {
      final Triple<Query, InferredRawQuery, DebugQuery> current = currentQuery();
      return new Triple<Query, InferredRawQuery, DebugQuery>(
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
      final Pair<Lattice, Element[]> result =
          currentQuery().first().getResultFor(expr);
      final int idx = result.first().indexOf(rcvrDecl);
      final HintDrop drop = HintDrop.newInformation(expr);
      drop.setCategorizingMessage(Messages.DSC_NON_NULL);
      drop.setMessage(Messages.RAWNESS,  result.second()[idx]);
      return null;
    }
    
    @Override
    public Void visitMethodBody(final IRNode b) {
      doAcceptForChildren(b);

      final Inferred inferredResult = currentQuery().second().getResultFor(b);
      for (final Pair<IRNode, Element> p : inferredResult) {
        final IRNode varDecl = p.first();
        final RawPromiseDrop pd = NonNullRules.getRaw(varDecl);
        final Element annotation = inferredResult.injectAnnotation(pd);
        final Element inferred = p.second();
        final boolean isGood = inferredResult.lessEq(inferred, annotation);
        ResultsBuilder.createResult(varDecl, pd, isGood, 910, 911, inferred);
      }
        
      final String state = currentQuery().third().getResultFor(b);
      final HintDrop drop = HintDrop.newInformation(b);
      drop.setCategorizingMessage(Messages.DSC_NON_NULL);
      drop.setMessage(Messages.RAW_STATE, state);

      return null;
    }
    
    @Override
    public void handleConstructorCall(final IRNode expr) {
      final IRNode rcvrDecl = JavaPromise.getReceiverNode(getEnclosingDecl());
      final Pair<Lattice, Element[]> result =
          currentQuery().first().getResultFor(expr);
      final int idx = result.first().indexOf(rcvrDecl);
      final HintDrop drop = HintDrop.newInformation(expr);
      drop.setCategorizingMessage(Messages.DSC_NON_NULL);
      drop.setMessage(Messages.RAWNESS, result.second()[idx]);
      
      super.handleConstructorCall(expr);
    }
  }

  @Override
  protected void clearCaches() {
    // Nothing to do
  }
}
