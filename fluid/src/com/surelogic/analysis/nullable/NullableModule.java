package com.surelogic.analysis.nullable;

import java.util.Map;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.StackEvaluatingAnalysisWithInference.InferredVarState;
import com.surelogic.analysis.Unused;
import com.surelogic.analysis.nullable.DefinitelyAssignedAnalysis;
import com.surelogic.analysis.nullable.DefinitelyAssignedAnalysis.AllResultsQuery;
import com.surelogic.analysis.nullable.NullableModule.AnalysisBundle.QueryBundle;
import com.surelogic.analysis.nullable.combined.NonNullRawLattice;
import com.surelogic.analysis.nullable.combined.NonNullRawLattice.Element;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.Inferred;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.InferredQuery;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.dropsea.ir.AbstractSeaConsistencyProofHook;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NullablePromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.RawPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public final class NullableModule extends AbstractWholeIRAnalysis<NullableModule.AnalysisBundle, Unused>{
  private static final int NON_NULL_LOCAL_CATEGORY = 900;
  
  private static final int DEFINITELY_ASSIGNED = 900;
  private static final int NOT_DEFINITELY_ASSIGNED = 901;
  private static final int DEFINITELY_ASSIGNED_STATIC = 902;
  private static final int NOT_DEFINITELY_ASSIGNED_STATIC = 903;
  
  private static final int LOCAL_NON_NULL = 935;
  
  private static final int TRIVIAL_METHOD_RETURN = 970;
  private static final int TRIVIAL_PARAMETER = 971;
  private static final int TRIVIAL_FIELD = 972;
  private static final int TRIVIAL_UNKNOWN = 973;
  
  
  
  public NullableModule() {
    super("Nullable");
  }

  @Override
  public void init(final IIRAnalysisEnvironment env) {
    Sea.getDefault().addConsistencyProofHook(new AbstractSeaConsistencyProofHook() {
      @Override
      public void preConsistencyProof(final Sea sea) {
        /* Find all the @NonNull, @Nullable, and @Raw annotations that do not have
         * any results under them.
         */
        addTrivialResults(sea, NonNullPromiseDrop.class);
        addTrivialResults(sea, NullablePromiseDrop.class);
        addTrivialResults(sea, RawPromiseDrop.class);
      }
    });
  }
  
  private static <C extends PromiseDrop<?>> void addTrivialResults(
      final Sea sea, final Class<C> T) {
    for (final C p : sea.getDropsOfType(T)) {
      if (p.getCheckedBy().isEmpty()) {
        final ResultDrop r = new ResultDrop(p.getNode());
        r.setConsistent();
        r.addChecked(p);
        
        final Operator op = JJNode.tree.getOperator(p.getPromisedFor());
        if (ReturnValueDeclaration.prototype.includes(op)) {
          r.setMessage(TRIVIAL_METHOD_RETURN);
        } else if (ParameterDeclaration.prototype.includes(op) ||
            ReceiverDeclaration.prototype.includes(op)) {
          r.setMessage(TRIVIAL_PARAMETER);
        } else if (VariableDeclarator.prototype.includes(op)) {
          r.setMessage(TRIVIAL_FIELD);
        } else {
          r.setMessage(TRIVIAL_UNKNOWN);
        }
      }
    }
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
      processFields(ConstructorDeclaration.getBody(cdecl), false,
          cdecl, DEFINITELY_ASSIGNED, NOT_DEFINITELY_ASSIGNED,
          JavaNames.genSimpleMethodConstructorName(cdecl));
    }
    
    @Override
    public Void visitMethodBody(final IRNode body) {
      doAcceptForChildren(body);
      final Inferred result = currentQuery().getInferred(body);
      for (final InferredVarState<Element> p : result) {
        /* 
         * Cannot put proposed promises on local variable declarations.
         * use info drops instead.
         */
        final IRNode varDecl = p.getLocal();
        if (p.getState() == NonNullRawLattice.NOT_NULL) {
          final IRNode where = JJNode.tree.getParent(JJNode.tree.getParent(varDecl));
          final HintDrop hint = HintDrop.newInformation(where);
          hint.setCategorizingMessage(NON_NULL_LOCAL_CATEGORY);
          hint.setMessage(LOCAL_NON_NULL, VariableDeclarator.getId(varDecl));
        }
      }
      return null;
    }

    @Override
    protected void handleClassInitDeclaration(
        final IRNode classBody, final IRNode node) {
      processFields(classBody, true, classBody,
          DEFINITELY_ASSIGNED_STATIC, NOT_DEFINITELY_ASSIGNED_STATIC,
          "<clinit>");
    }

  
  
    private void processFields(
        final IRNode analysisNode, final boolean useStatic,
        final IRNode resultNode, final int goodMsg, final int badMsg,
        final String name) {
      final Map<IRNode, Boolean> fieldsStatus = 
          currentQuery().getDefinitelyAssigned(analysisNode);
      for (final Map.Entry<IRNode, Boolean> e : fieldsStatus.entrySet()) {
        final IRNode fieldDecl = e.getKey();
        if (TypeUtil.isStatic(fieldDecl) == useStatic) {
          final NonNullPromiseDrop pd = NonNullRules.getNonNull(fieldDecl);
          if (pd != null) {
            final boolean isDefinitelyAssigned = e.getValue().booleanValue();
            ResultsBuilder.createResult(resultNode, pd, isDefinitelyAssigned,
                goodMsg, badMsg, name);
          }
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
    private final NonNullRawTypeAnalysis nonNullRawType;
    private final NonNullTypeChecker typeChecker;
    
    
    
    private AnalysisBundle(final IBinder b) {
      binder = b;
      definiteAssignment = new DefinitelyAssignedAnalysis(b, false);
      nonNullRawType = new NonNullRawTypeAnalysis(b);
      typeChecker = new NonNullTypeChecker(b, nonNullRawType);
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
      nonNullRawType.clearCaches();
    }
    
    public void clear() {
      definiteAssignment.clear();
    }
    
    
    
    final class QueryBundle {
      private final AllResultsQuery allResultsQuery;
      private final InferredQuery inferredQuery;
      
      public QueryBundle(final IRNode flowUnit) {
        allResultsQuery = definiteAssignment.getAllResultsQuery(flowUnit);
        inferredQuery = nonNullRawType.getInferredQuery(flowUnit);
      }
      
      private QueryBundle(final QueryBundle qb, final IRNode caller) {
        allResultsQuery = qb.allResultsQuery.getSubAnalysisQuery(caller);
        inferredQuery = qb.inferredQuery.getSubAnalysisQuery(caller);
      }
      
      public QueryBundle getSubAnalysisQuery(final IRNode caller) {
        return new QueryBundle(this, caller);
      }
      
      public Map<IRNode, Boolean> getDefinitelyAssigned(final IRNode node) {
        return allResultsQuery.getResultFor(node);
      }
      
      public Inferred getInferred(final IRNode node) {
        return inferredQuery.getResultFor(node);
      }
    }
  }
}
