package com.surelogic.analysis.nullable;

import java.util.Map;
import java.util.Set;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.StackEvaluatingAnalysisWithInference.Assignment;
import com.surelogic.analysis.StackEvaluatingAnalysisWithInference.InferredVarStateQuery;
import com.surelogic.analysis.StackEvaluatingAnalysisWithInference.Result;
import com.surelogic.analysis.Unused;
import com.surelogic.analysis.nullable.DefinitelyAssignedAnalysis;
import com.surelogic.analysis.nullable.DefinitelyAssignedAnalysis.AllResultsQuery;
import com.surelogic.analysis.nullable.NonNullAnalysis.NullInfo;
import com.surelogic.analysis.nullable.NonNullAnalysis.NullLattice;
import com.surelogic.analysis.nullable.NullableModule.AnalysisBundle.QueryBundle;
import com.surelogic.analysis.nullable.RawLattice.Element;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.RawPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.util.Triple;

public final class NullableModule extends AbstractWholeIRAnalysis<NullableModule.AnalysisBundle, Unused>{
  private static final int DEFINITELY_ASSIGNED = 900;
  private static final int NOT_DEFINITELY_ASSIGNED = 901;
  
  private static final int RAW_LOCAL_GOOD = 910;
  private static final int RAW_LOCAL_BAD = 911;
  private static final int ASSIGNMENT = 912;
  
  
  
  public NullableModule() {
    super("Nullable");
  }

  @Override
  protected AnalysisBundle constructIRAnalysis(final IBinder binder) {
    return new AnalysisBundle(binder);
  }

  @Override
  protected boolean doAnalysisOnAFile(final IIRAnalysisEnvironment env,
      final CUDrop cud, final IRNode compUnit) {
    runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
      @Override
      public void run() {
        visitCompilationUnit(compUnit);
      }
    });
    return true;
  }

  protected void visitCompilationUnit(final IRNode compUnit) {
    final Visitor v = new Visitor();
    v.doAccept(compUnit);
    getAnalysis().clear();
  }
  
  
  
  private final class Visitor extends AbstractJavaAnalysisDriver<QueryBundle> {
    @Override
    protected QueryBundle createNewQuery(final IRNode decl) {
      return getAnalysis().new QueryBundle(decl);
    }

    @Override
    protected QueryBundle createSubQuery(final IRNode caller) {
      return currentQuery().getSubAnalysisQuery(caller);
    }


    
    @Override
    protected void handleConstructorDeclaration(final IRNode cdecl) {
      doAcceptForChildren(cdecl);

      final Map<IRNode, Boolean> fieldsStatus = 
          currentQuery().getDefinitelyAssigned(ConstructorDeclaration.getBody(cdecl));
      for (final Map.Entry<IRNode, Boolean> e : fieldsStatus.entrySet()) {
        final IRNode fieldDecl = e.getKey();
        final NonNullPromiseDrop pd = NonNullRules.getNonNull(fieldDecl);
        if (pd != null) {
          final boolean isDefinitelyAssigned = e.getValue().booleanValue();
          ResultsBuilder.createResult(cdecl, pd, isDefinitelyAssigned,
              DEFINITELY_ASSIGNED, NOT_DEFINITELY_ASSIGNED,
              JavaNames.genSimpleMethodConstructorName(cdecl));
        }
      }
    }
    
    @Override
    public Void visitMethodBody(final IRNode body) {
      doAcceptForChildren(body);
      
      final Result<RawLattice.Element, RawLattice> inferredRaw = currentQuery().getInferredRaw(body);
      for (final Triple<IRNode, Element, Set<Assignment<Element>>> p : inferredRaw) {
        final IRNode varDecl = p.first();
        final RawPromiseDrop pd = NonNullRules.getRaw(varDecl);
        final Element annotation = inferredRaw.getLattice().injectPromiseDrop(pd);
        final Element inferred = p.second();
        final boolean isGood = inferredRaw.lessEq(inferred, annotation);
        final ResultDrop rd = ResultsBuilder.createResult(
            varDecl, pd, isGood, RAW_LOCAL_GOOD, RAW_LOCAL_BAD, inferred);
        
        for (final Assignment<Element> a : p.third()) {
          final HintDrop hint = HintDrop.newInformation(a.first());
          hint.setMessage(ASSIGNMENT, a.second());
          rd.addDependent(hint);
        }
      }
      
      final Result<NullInfo, NullLattice> inferredNull = currentQuery().getInferredNull(body);
      for (final Triple<IRNode, NullInfo, Set<Assignment<NullInfo>>> p : inferredNull) {
        final IRNode varDecl = p.first();
        final NullInfo inferred = p.second();
        final boolean isGood = inferredNull.lessEq(inferred, NullInfo.NOTNULL);
        final ResultDrop rd = ResultsBuilder.createResult(
            varDecl, NonNullRules.getNonNull(varDecl), isGood,
            RAW_LOCAL_GOOD, RAW_LOCAL_BAD, inferred);
        
        for (final Assignment<NullInfo> a : p.third()) {
          final HintDrop hint = HintDrop.newInformation(a.first());
          hint.setMessage(ASSIGNMENT, a.second());
          rd.addDependent(hint);
        }
      }
      return null;
    }
  }

  @Override
  protected void clearCaches() {
    // Nothing to do
  }
  
  
  
  static final class AnalysisBundle implements IBinderClient {
    private final IBinder binder;
    private final DefinitelyAssignedAnalysis definiteAssignment;
    private final RawTypeAnalysis rawType;
    private final NonNullAnalysis nonNull;
    
    
    
    private AnalysisBundle(final IBinder b) {
      binder = b;
      definiteAssignment = new DefinitelyAssignedAnalysis(b, false);
      rawType = new RawTypeAnalysis(b);
      nonNull = new NonNullAnalysis(b);
    }
    
    @Override
    public IBinder getBinder() {
      return binder;
    }

    @Override
    public void clearCaches() {
      definiteAssignment.clearCaches();
      rawType.clearCaches();
    }
    
    public void clear() {
      definiteAssignment.clear();
    }
    
    
    
    final class QueryBundle {
      private final AllResultsQuery allResultsQuery;
      private final InferredVarStateQuery<RawLattice.Element, RawLattice, ?, ?> inferredRawQuery;
      private final InferredVarStateQuery<NullInfo, NullLattice, ?, ?> inferredNullQuery;
      
      public QueryBundle(final IRNode flowUnit) {
        allResultsQuery = definiteAssignment.getAllResultsQuery(flowUnit);
        inferredRawQuery = rawType.getInferredVarStateQuery(flowUnit);
        inferredNullQuery = nonNull.getInferredVarStateQuery(flowUnit);
      }
      
      private QueryBundle(final QueryBundle qb, final IRNode caller) {
        allResultsQuery = qb.allResultsQuery.getSubAnalysisQuery(caller);
        inferredRawQuery = qb.inferredRawQuery.getSubAnalysisQuery(caller);
        inferredNullQuery = qb.inferredNullQuery.getSubAnalysisQuery(caller);
      }
      
      public QueryBundle getSubAnalysisQuery(final IRNode caller) {
        return new QueryBundle(this, caller);
      }
      
      public Map<IRNode, Boolean> getDefinitelyAssigned(final IRNode node) {
        return allResultsQuery.getResultFor(node);
      }
      
      public Result<RawLattice.Element, RawLattice> getInferredRaw(final IRNode node) {
        return inferredRawQuery.getResultFor(node);
      }
      
      public Result<NullInfo, NullLattice> getInferredNull(final IRNode node) {
        return inferredNullQuery.getResultFor(node);
      }
    }
  }
}
