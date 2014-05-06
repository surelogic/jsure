package com.surelogic.analysis.nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.surelogic.NonNull;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.ConcurrencyType;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.granules.FlowUnitGranulator;
import com.surelogic.analysis.granules.FlowUnitGranule;
import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.analysis.nullable.DefinitelyAssignedAnalysis;
import com.surelogic.analysis.nullable.DefinitelyAssignedAnalysis.AllResultsQuery;
import com.surelogic.analysis.nullable.NonNullRawLattice.Element;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.Inferred;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.InferredQuery;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.InferredVarState;
import com.surelogic.analysis.nullable.NullableModule2.AnalysisBundle.QueryBundle;
import com.surelogic.analysis.visitors.FlowUnitVisitor;
import com.surelogic.analysis.visitors.SuperVisitor;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.common.concurrent.ConcurrentHashSet;
import com.surelogic.common.util.FilterIterator;
import com.surelogic.common.util.IteratorUtil;
import com.surelogic.dropsea.ir.AbstractSeaConsistencyProofHook;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Builder;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NullablePromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.RawPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
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
import edu.uwm.cs.fluid.control.FlowAnalysis.AnalysisGaveUp;

public final class NullableModule2 extends AbstractWholeIRAnalysis<NullableModule2.AnalysisBundle, FlowUnitGranule>{
  private static final long NANO_SECONDS_PER_SECOND = 1000000000L;

  private static final int NON_NULL_LOCAL_CATEGORY = 900;
  private static final int TIME_OUT_CATEGORY = 901;
  
  private static final int DEFINITELY_ASSIGNED = 900;
  private static final int NOT_DEFINITELY_ASSIGNED = 901;
  private static final int DEFINITELY_ASSIGNED_STATIC = 902;
  private static final int NOT_DEFINITELY_ASSIGNED_STATIC = 903;
  
  private static final int LOCAL_NON_NULL = 935;
  
  private static final int TRIVIAL_METHOD_RETURN = 970;
  private static final int TRIVIAL_PARAMETER = 971;
  private static final int TRIVIAL_FIELD = 972;
  private static final int TRIVIAL_NULLABLE_RETURN = 973;
  private static final int TRIVIAL_UNKNOWN = 974;
  
  private static final int TIME_OUT = 980;
  
  
  
  private static final Set<PromiseDrop<?>> createdVirtualAnnotations = 
      new ConcurrentHashSet<PromiseDrop<?>>();
  
  
  public NullableModule2() {
    super("Nullable");
  }

  @Override
  public ConcurrencyType runInParallel() {
    return ConcurrencyType.NEVER;
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
      /*
       * Only add trivial results to unsupported promises that aren't assumed
       * or from binary files.  Drops that are assumed or from a binary file
       * we WANT to be marked with a red dot.
       */
      if (p.getCheckedBy().isEmpty() && !p.isAssumed() &&
          !TypeUtil.isBinary(p.getPromisedFor())) {
        final ResultDrop r = new ResultDrop(p.getNode());
        r.setConsistent();
        r.addChecked(p);
        
        final Operator op = JJNode.tree.getOperator(p.getPromisedFor());
        if (ReturnValueDeclaration.prototype.includes(op)) {
          r.setMessage(
              p.isVirtual() ? TRIVIAL_NULLABLE_RETURN : TRIVIAL_METHOD_RETURN);
        } else if (ParameterDeclaration.prototype.includes(op) ||
            ReceiverDeclaration.prototype.includes(op)) {
          r.setMessage(TRIVIAL_PARAMETER);
        } else if (VariableDeclarator.prototype.includes(op)) {
          r.setMessage(TRIVIAL_FIELD);
        } else {
          r.setMessage(TRIVIAL_UNKNOWN); // Shouldn't get here
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
    visitCompilationUnit(compUnit);
    return true;
  }

  protected void visitCompilationUnit(final IRNode compUnit) {
    final Driver driver = new Driver();
    driver.doAccept(compUnit);
    getAnalysis().clear();
  }

  @Override
  public IAnalysisGranulator<FlowUnitGranule> getGranulator() {
    return FlowUnitGranulator.prototype;
  }
  
  @Override
  protected boolean doAnalysisOnGranule_wrapped(
      final IIRAnalysisEnvironment env, final FlowUnitGranule g) {
	/*
	if (g.getLabel().startsWith("android.util.Base64")) {
		System.out.println("Found Base64 granule: "+g.getLabel());
	}
	*/
	AnalysisBundle a = getAnalysis();
	try {
		a.execute(g);
	} finally {		
		a.clear();
	}
    return true; 
  }
  
  
  
  private final class Driver extends SuperVisitor {
    public Driver() {
      super(true);
    }
    
    @Override
    protected List<FlowUnitVisitor<?>> createSubVisitors() {
      return getAnalysis().getVisitors();
    }


    
    private JavaComponentFactory jcf = null;
    
    @Override
    protected void enteringEnclosingDecl(
        final IRNode newDecl, final IRNode anonClassDecl) {
      jcf = JavaComponentFactory.startUse();
    }
    
    @Override
    protected final void leavingEnclosingDecl(
        final IRNode oldDecl, final IRNode returningTo) {
      JavaComponentFactory.finishUse(jcf);
      jcf = null;
    }
  }
  
  
  
  private static final class DetailVisitor extends FlowUnitVisitor<QueryBundle> {
    private final AnalysisBundle bundle;
    
    public DetailVisitor(final AnalysisBundle b) {
      super(true);
      bundle = b;
    }
    
    @Override
    protected QueryBundle createNewQuery(final IRNode decl) {
      return bundle. new QueryBundle(decl);
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
      final long startTime = System.nanoTime();
      try {
        final Inferred result = currentQuery().getInferred(body);
        for (final InferredVarState p : result) {
          /* 
           * Cannot put proposed promises on local variable declarations.
           * use info drops instead.
           */
          final IRNode varDecl = p.getLocal();
          final Element state = p.getState();
          if (ReturnValueDeclaration.prototype.includes(varDecl)) { // method return value
            if (state == NonNullRawLattice.NOT_NULL &&
                NonNullRules.getNonNull(varDecl) == null &&
                NonNullRules.getNullable(varDecl) == null) {
              /* Return value is unannotated and known to be NOT_NULL.  Propose
               * to annotate the node with @NonNull.
               * 
               * Need to make a virtual @Nullable annotation to attach the proposal to.
               */
              final NullablePromiseDrop pd =
                  NullableUtils.attachVirtualNullable(varDecl, createdVirtualAnnotations);
              pd.addProposal(new Builder(NonNull.class, varDecl, varDecl).build());
              
            }
          } else { // local variable
            if (state == NonNullRawLattice.NOT_NULL &&
                NonNullRules.getNonNull(varDecl) == null) {
              final IRNode where = JJNode.tree.getParent(JJNode.tree.getParent(varDecl));
              final HintDrop hint = HintDrop.newInformation(where);
              hint.setCategorizingMessage(NON_NULL_LOCAL_CATEGORY);
              hint.setMessage(LOCAL_NON_NULL, VariableDeclarator.getId(varDecl));
            }
          }
        }
      } catch (final AnalysisGaveUp e) {
        final long endTime = System.nanoTime();
        final long duration = endTime - startTime;
        final String name = JavaNames.genQualifiedMethodConstructorName(JJNode.tree.getParent(body));
//        final ResultDrop rd = new ResultDrop(JJNode.tree.getParent(body));
//        rd.setTimeout();
//        rd.setCategorizingMessage(TIME_OUT_CATEGORY);
//        rd.setMessage(TIME_OUT, e.timeOut / NANO_SECONDS_PER_SECOND,
//            name, duration / NANO_SECONDS_PER_SECOND);
//        // XXX: Need to attach this result to some promises!!!
        bundle.addTimeOut(body);
        
        final HintDrop hd = HintDrop.newWarning(JJNode.tree.getParent(body));
        hd.setCategorizingMessage(TIME_OUT_CATEGORY);
        hd.setMessage(TIME_OUT, e.timeOut / NANO_SECONDS_PER_SECOND,
            name, duration / NANO_SECONDS_PER_SECOND);
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
	  analyses.clearCaches();
  }
  
  @Override
  public void postAnalysis(final IIRProject p) {
    // Check deferred final fields for @NotNull status
    for (final IRNode varDecl : getAnalysis().getNotNullFields()) {
      /* 
       * We have an unannotated deferred final field that is only ever 
       * assigned NOT_NULL values.  Propose to make the field @NonNull
       * 
       * Need to make a virtual @Nullable annotation to attach the proposal to.
       */
      final NullablePromiseDrop pd = 
          NullableUtils.attachVirtualNullable(varDecl, createdVirtualAnnotations);
      pd.addProposal(new Builder(NonNull.class, varDecl, varDecl).build());
    }
    
    getAnalysis().clearGlobalCaches();
  }

  static final class AnalysisBundle implements IBinderClient {
    private final IBinder binder;
    private final DefinitelyAssignedAnalysis definiteAssignment;
    private final NonNullRawTypeAnalysis nonNullRawType;
    private final Set<IRNode> timedOutMethodBodies = new HashSet<IRNode>();
    private final Map<IRNode, Element> fieldInits = new HashMap<IRNode, Element>();
    private final DetailVisitor details;
    private final NonNullTypeCheckerSlave typeChecker;
    
    
    
    private AnalysisBundle(final IBinder b) {
      binder = b;
      definiteAssignment = new DefinitelyAssignedAnalysis(b, false);
      nonNullRawType = new NonNullRawTypeAnalysis(b);
      
      details = new DetailVisitor(this);
      typeChecker = new NonNullTypeCheckerSlave(b, nonNullRawType,
          timedOutMethodBodies, fieldInits, createdVirtualAnnotations);
    }
    
    @Override
    public IBinder getBinder() {
      return binder;
    }

    public Iterable<IRNode> getNotNullFields() {
      return new FilterIterator<Map.Entry<IRNode, Element>, IRNode>(
          fieldInits.entrySet().iterator()) {
        @Override
        protected Object select(final Entry<IRNode, Element> o) {
          return o.getValue() == NonNullRawLattice.NOT_NULL ? o.getKey() : IteratorUtil.noElement;
        }      
      };
    }
    
    @Override
    public void clearCaches() {
      definiteAssignment.clearCaches();
      nonNullRawType.clearCaches();
      timedOutMethodBodies.clear(); // TODO report these?
    }
    
    public void clear() {
      definiteAssignment.clear();
      nonNullRawType.clear();
      timedOutMethodBodies.clear(); // TODO report these?
    }
    
    public void clearGlobalCaches() {
      createdVirtualAnnotations.clear();
      fieldInits.clear();
    }
    
    public void addTimeOut(final IRNode mBody) {
      timedOutMethodBodies.add(mBody);
    }
    
    public Set<IRNode> getTimedOut() {
      return Collections.unmodifiableSet(timedOutMethodBodies);
    }
    
    public List<FlowUnitVisitor<?>> getVisitors() {
      final List <FlowUnitVisitor<?>> subs = new ArrayList<FlowUnitVisitor<?>>(2);
      subs.add(details);
      subs.add(typeChecker);
      return Collections.unmodifiableList(subs);
    }
    
    public void execute(final FlowUnitGranule granule) {
      granule.execute(details);
      granule.execute(typeChecker);
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
