package com.surelogic.analysis.nullable;

import java.util.Map;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.StackEvaluatingAnalysisWithInference.Assignment;
import com.surelogic.analysis.StackEvaluatingAnalysisWithInference.InferredVarState;
import com.surelogic.analysis.StackEvaluatingAnalysisWithInference.Result;
import com.surelogic.analysis.Unused;
import com.surelogic.analysis.nullable.DefinitelyAssignedAnalysis;
import com.surelogic.analysis.nullable.DefinitelyAssignedAnalysis.AllResultsQuery;
import com.surelogic.analysis.nullable.NonNullAnalysis.InferredNonNull;
import com.surelogic.analysis.nullable.NonNullAnalysis.InferredNonNullQuery;
import com.surelogic.analysis.nullable.NullableModule.AnalysisBundle.QueryBundle;
import com.surelogic.analysis.nullable.RawTypeAnalysis.InferredRaw;
import com.surelogic.analysis.nullable.RawTypeAnalysis.InferredRawQuery;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.NoInitialization;
import edu.uwm.cs.fluid.util.Lattice;

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
    
    getAnalysis().typeCheck(compUnit);
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
      processInferredResults(currentQuery().getInferredRaw(body));
      processInferredResults(currentQuery().getInferredNull(body));
      return null;
    }

    private <I, L extends Lattice<I>, P extends PromiseDrop<?>> 
    void processInferredResults(final Result<I, L, P> result) {
      for (final InferredVarState<I> p : result) {
        final IRNode varDecl = p.getLocal();
        final P pd = result.getPromiseDrop(varDecl);
        final I annotation = result.injectPromiseDrop(pd);
        final I inferred = p.getState();
        final boolean isGood = result.lessEq(inferred, annotation);
        final ResultDrop rd = ResultsBuilder.createResult(
            varDecl, pd, isGood, RAW_LOCAL_GOOD, RAW_LOCAL_BAD, inferred);
        
        for (final Assignment<I> a : p.getAssignments()) {
          final IRNode src = a.getWhere();
          String unparse;
          if (Initialization.prototype.includes(src)) {
            unparse = DebugUnparser.toString(Initialization.getValue(src), -1);
          } else if (NoInitialization.prototype.includes(src)) {
            unparse = "null (by default)";
          } else {
            unparse = DebugUnparser.toString(src, -1);
          }
          if (unparse.length() > 39) {
            unparse = unparse.substring(0, 39) + "\u2026";
          }
          final HintDrop hint = HintDrop.newInformation(src);
          hint.setMessage(ASSIGNMENT, a.getState(), unparse);
          rd.addDependent(hint);
        }
      }      
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
    private final NonNullTypeChecker typeChecker;
    
    
    
    private AnalysisBundle(final IBinder b) {
      binder = b;
      definiteAssignment = new DefinitelyAssignedAnalysis(b, false);
      rawType = new RawTypeAnalysis(b);
      nonNull = new NonNullAnalysis(b);
      typeChecker = new NonNullTypeChecker(b, nonNull, rawType);
    }
    
    public void typeCheck(final IRNode cu) {
      typeChecker.doAccept(cu);
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
      private final InferredRawQuery inferredRawQuery;
      private final InferredNonNullQuery inferredNullQuery;
      
      public QueryBundle(final IRNode flowUnit) {
        allResultsQuery = definiteAssignment.getAllResultsQuery(flowUnit);
        inferredRawQuery = rawType.getInferredRawQuery(flowUnit);
        inferredNullQuery = nonNull.getInferredNonNullQuery(flowUnit);
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
      
      public InferredRaw getInferredRaw(final IRNode node) {
        return inferredRawQuery.getResultFor(node);
      }
      
      public InferredNonNull getInferredNull(final IRNode node) {
        return inferredNullQuery.getResultFor(node);
      }
    }
  }
}
