package com.surelogic.analysis.nullable;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.surelogic.Initialized;
import com.surelogic.NonNull;
import com.surelogic.analysis.AbstractThisExpressionBinder;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.nullable.DefinitelyAssignedFieldAnalysis.AllResultsQuery;
import com.surelogic.analysis.nullable.NonNullRawLattice.ClassElement;
import com.surelogic.analysis.nullable.NonNullRawLattice.Element;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.Base;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.Kind;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.Source;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.StackQuery;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.StackQueryResult;
import com.surelogic.analysis.type.checker.QualifiedTypeCheckerSlave;
import com.surelogic.analysis.visitors.InstanceInitAction;
import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.common.SLUtility;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IKeyValue;
import com.surelogic.dropsea.KeyValueUtility;
import com.surelogic.dropsea.ir.AnalysisResultDrop;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Builder;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NullablePromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.TrackPartiallyInitializedPromiseDrop;
import com.surelogic.dropsea.irfree.DiffHeuristics;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.Arguments;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.LambdaExpression;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.ReferenceType;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.uwm.cs.fluid.control.FlowAnalysis.AnalysisGaveUp;

public final class NonNullTypeCheckerSlave
extends QualifiedTypeCheckerSlave<com.surelogic.analysis.ThisExpressionBinder, NonNullTypeCheckerSlave.Queries> {
  private static final int POSSIBLY_NULL = 915;
  private static final int POSSIBLY_NULL_UNBOX = 916;
  private static final int READ_FROM = 917;
  
  private static final int ACCEPTABLE_FIELD = 920;
  private static final int UNACCEPTABLE_FIELD = 921;
  private static final int ACCEPTABLE_VARIABLE = 922;
  private static final int UNACCEPTABLE_VARIABLE = 923;
  private static final int ACCEPTABLE_PARAMETER = 924;
  private static final int UNACCEPTABLE_PARAMETER = 925;
  private static final int ACCEPTABLE_RETURN = 926;
  private static final int UNACCEPTABLE_RETURN = 927;
  private static final int ACCEPTABLE_RECEIVER = 928;
  private static final int UNACCEPTABLE_RECEIVER = 929;
  private static final int UNACCEPTABLE_RAW_RECEIVER = 930;
  
  private static final int RAW_INTO_NULLABLE = 932;
  private static final int USE_CAST_NULLABLE = 933;
  
  

  private enum LValue {
    FIELD {
      @Override
      public String toString() { return "field"; }
      @Override 
      public int getAssuredMessage() { return ACCEPTABLE_FIELD; }
      @Override
      public int getFailedMessage() { return UNACCEPTABLE_FIELD; }
      @Override
      public String getName(final IRNode expr) {
        return VariableDeclarator.getId(expr);
      }
    },
    VARIABLE {
      @Override
      public String toString() { return "variable"; }
      @Override 
      public int getAssuredMessage() { return ACCEPTABLE_VARIABLE; }
      @Override
      public int getFailedMessage() { return UNACCEPTABLE_VARIABLE; }
      @Override
      public String getName(final IRNode expr) {
        return VariableDeclarator.getId(expr);
      }
    },
    PARAMETER {
      @Override
      public String toString() { return "parameter"; }
      @Override 
      public int getAssuredMessage() { return ACCEPTABLE_PARAMETER; }
      @Override
      public int getFailedMessage() { return UNACCEPTABLE_PARAMETER; }
      @Override
      public String getName(final IRNode expr) {
        return ParameterDeclaration.getId(expr);
      }
    },
    RETURN {
      @Override
      public String toString() { return "return value"; }
      @Override 
      public int getAssuredMessage() { return ACCEPTABLE_RETURN; }
      @Override
      public int getFailedMessage() { return UNACCEPTABLE_RETURN; }
      @Override
      public String getName(final IRNode expr) { return null; }
    },
    RECEIVER {
      @Override
      public String toString() { return "receiver"; }
      @Override 
      public int getAssuredMessage() { return ACCEPTABLE_RECEIVER; }
      @Override
      public int getFailedMessage() { return UNACCEPTABLE_RECEIVER; }
      @Override
      public String getName(final IRNode expr) { return null; }
    };
    
    
    
    public abstract int getAssuredMessage();
    public abstract int getFailedMessage();
    public abstract String getName(IRNode expr);
  }
  
  
  
  final class Queries {
    private final StackQuery stackQuery;
    private final AllResultsQuery allResultsQuery;
    
    public Queries(final IRNode flowUnit) {
      stackQuery = nonNullRawTypeAnalysis.getStackQuery(flowUnit);
      allResultsQuery = definitelyAssignedAnalysis.getAllResultsQuery(flowUnit);
    }
    
    private Queries(final Queries q, final IRNode caller) {
      stackQuery = q.stackQuery.getSubAnalysisQuery(caller);
      allResultsQuery = q.allResultsQuery.getSubAnalysisQuery(caller);
    }
    
    public Queries getSubAnalysisQuery(final IRNode caller) {
      return new Queries(this, caller);
    }
    
    public StackQueryResult getStackResult(final IRNode where) {
      return stackQuery.getResultFor(where);
    }
    
    public boolean isNotDefinitelyAssigned(final IRNode where, final IRNode fdecl) {
      final Map<IRNode, Boolean> fieldMap = allResultsQuery.getResultFor(where);
      final Boolean boolean1 = fieldMap.get(fdecl);
      return boolean1 != null && !boolean1;
    }
  }
  
  
  
  private final NonNullRawTypeAnalysis nonNullRawTypeAnalysis;
  private final DefinitelyAssignedFieldAnalysis definitelyAssignedAnalysis;
  
  private final Set<IRNode> badMethodBodies;
  
  private IRNode receiverDecl = null;

  private final Set<PromiseDrop<?>> createdVirtualAnnotations;
  
  private final Map<IRNode, Element> fieldInits;
  
  
  
  public NonNullTypeCheckerSlave(final IBinder b,
      final NonNullRawTypeAnalysis nonNullRaw,
      final DefinitelyAssignedFieldAnalysis defAssign,
      final Set<IRNode> badMethodBodies,
      final Map<IRNode, Element> fields,
      final Set<PromiseDrop<?>> cva) {
    super(b);
    nonNullRawTypeAnalysis = nonNullRaw;
    definitelyAssignedAnalysis = defAssign;
    this.badMethodBodies = badMethodBodies;
    fieldInits = fields;
    createdVirtualAnnotations = cva;
  }
  
  @Override
  protected com.surelogic.analysis.ThisExpressionBinder initBinder(final IBinder b) {
    return new ThisExpressionBinder(b);
  }
  
  
  
  // ======================================================================
  // == Manage the binding of this expression
  // ======================================================================
  
  @Override
  protected void handleMethodDeclaration(final IRNode mdecl) {
    final IRNode oldReceiverDecl = receiverDecl;
    try {
      receiverDecl = JavaPromise.getReceiverNodeOrNull(mdecl);
      doAcceptForChildren(mdecl);
    } finally {
      receiverDecl = oldReceiverDecl;
    }
  }
  
  @Override
  protected void handleConstructorDeclaration(final IRNode cdecl) {
    final IRNode oldReceiverDecl = receiverDecl;
    try {
      receiverDecl = JavaPromise.getReceiverNodeOrNull(cdecl);
      doAcceptForChildren(cdecl);
    } finally {
      receiverDecl = oldReceiverDecl;
    }
  }
  
  @Override
  protected InstanceInitAction getAnonClassInitAction(
      final IRNode expr, final IRNode classBody) {
    return new InstanceInitAction() {
      final IRNode oldReceiverDecl = receiverDecl;
      
      @Override
      public void tryBefore() {
        receiverDecl = JavaPromise.getReceiverNodeOrNull(getEnclosingDecl());
      }
      
      @Override
      public void finallyAfter() {
        receiverDecl = oldReceiverDecl;
      }
      
      @Override
      public void afterVisit() {
        // does nothing
      }
    };
  }
  
  private final class ThisExpressionBinder extends AbstractThisExpressionBinder {
    public ThisExpressionBinder(final IBinder b) {
      super(b);
    }

    @Override
    protected IRNode bindReceiver(IRNode node) {
      return receiverDecl;
    }
    
    @Override
    protected IRNode bindQualifiedReceiver(IRNode outerType, IRNode node) {
      return JavaPromise.getQualifiedReceiverNodeByName(getEnclosingDecl(), outerType);
    }    
  }

  // ======================================================================

  @Override
  public Void visitMethodBody(final IRNode mBody) {
    if (!badMethodBodies.contains(mBody)) {
      super.visitMethodBody(mBody);
    }
    return null;
  }
  
  
  
  @Override
  protected Queries createNewQuery(final IRNode decl) {
    return new Queries(decl);
  }

  @Override
  protected Queries createSubQuery(final IRNode caller) {
    return currentQuery().getSubAnalysisQuery(caller);
  }



  private void checkForNull(final IRNode expr) {
    checkForNull(expr, false);
  }
  
  private void checkForNull(final IRNode expr, final boolean isUnbox) {
    try {
      final StackQueryResult queryResult = currentQuery().getStackResult(expr);
      final Element state = queryResult.getValue();    
      if (state == NonNullRawLattice.MAYBE_NULL || state == NonNullRawLattice.NULL) {
        // Hunt for any @Nullable annotations 
        buildWarningResults(isUnbox, expr, 
            queryResult.getSources(), new LinkedList<IRNode>());
      }
    } catch (AnalysisGaveUp e) {
      // do nothing
    }
  }
  
  private void buildWarningResults(
      final boolean isUnbox,
      final IRNode expr,
      final Set<Source> sources,
      final Deque<IRNode> chain) {
    // Hunt for any @Nullable annotations 
    for (final Source src : sources) {
      final Kind k = src.first();
      final IRNode where = src.second();
      
      if (k == Kind.VAR_USE || k == Kind.THIS_EXPR) {
        final IRNode vd = k.bind(where, binder);
        final StackQueryResult newQuery = currentQuery().getStackResult(where);
        final Base varValue = newQuery.lookupVar(vd);
        chain.addLast(where);
        buildWarningResults(isUnbox, expr, varValue.second(), chain);
        chain.removeLast();    
      } else if (k != Kind.NO_VALUE) {
        final IRNode annotatedNode = k.getAnnotatedNode(binder, where);
        final PromiseDrop<?> pd = getAnnotationForProof(annotatedNode);
        /* Add a warning if the promise is a REAL @Nullable annotation,
         * but skip the warning if the @Nullable is a virtual one added by
         * checkAssignability for reporting problems with @Raw.
         */
        if (pd instanceof NullablePromiseDrop && !createdVirtualAnnotations.contains(pd)) { // N.B. null is never an instance of anything
          HintDrop hd = pd.addWarningHint(
              expr, isUnbox ? POSSIBLY_NULL_UNBOX : POSSIBLY_NULL);
          for (final IRNode readFrom : chain) {
            hd = hd.addInformationHint(
                readFrom, READ_FROM, DebugUnparser.toString(readFrom));
          }
          
          hd.addInformationHint(
              where, k.getMessage(),
              src.third().getAnnotation(), k.unparse(where));
        } else if (pd instanceof NonNullPromiseDrop && k == Kind.RAW_FIELD_REF) {
          /* We get here when we have the use of a @NonNull field
           * being referenced through an @Initialized reference.  So here
           * we have the case of a @NonNull field being possibly null.  This 
           * is a very dangerous situation, so we report an error.
           * 
           * But if we are inside a constructor, things are trickier because all
           * the fields declared in the class are going to trigger this error
           * because the receiver is RAW.  We only report the error if the 
           * field is not definitely assigned.   
           */
          if (!isInsideConstructor() || currentQuery().isNotDefinitelyAssigned(where, annotatedNode)) {
            final ResultDrop rd = ResultsBuilder.createResult(
                false, pd, expr, isUnbox ? POSSIBLY_NULL_UNBOX : POSSIBLY_NULL);
            
            Drop d = rd;
            for (final IRNode readFrom : chain) {
              d = d.addInformationHint(
                  readFrom, READ_FROM, DebugUnparser.toString(readFrom));
            }
  
            d.addInformationHint(
                where, k.getMessage(),
                src.third().getAnnotation(), k.unparse(where));
          }
        }
        
        /* If the annotation is @Nullable or non-existent, and the kind is
         * FORMAL_PARAMETER or METHOD_RETURN, we propose to make the
         * parameter/return @NonNull.
         */
        if ((k == Kind.FORMAL_PARAMETER || k == Kind.METHOD_RETURN) &&
            (pd == null || pd instanceof NullablePromiseDrop)) {
          // Don't do it if the thing to be annotated is in a class file
          if (!TypeUtil.isBinary(
              k == Kind.METHOD_RETURN ?
                  JavaPromise.getPromisedFor(annotatedNode) : annotatedNode)) {
            NullableUtils.createCodeProposal(
                new Builder(NonNull.class, annotatedNode, expr));
          }
        }
      }
    }
  }
  
  private PromiseDrop<?> getAnnotationForProof(final IRNode n) {
    PromiseDrop<?> pd = getAnnotationToAssure(n);
    if (pd == null) pd = NonNullRules.getCast(n);
    return pd;
  }
  
  private PromiseDrop<?> getAnnotationToAssure(final IRNode n) {
    PromiseDrop<?> pd = NonNullRules.getRaw(n);
    if (pd == null) pd = NonNullRules.getNonNull(n);
    if (pd == null) pd = NonNullRules.getNullable(n);
    return pd;
  }
  
  private void checkAssignability(
      final IRNode expr, final IRNode decl, final LValue kind, final IRNode declTypeNode) {
    if (ReferenceType.prototype.includes(declTypeNode)) {
      checkAssignability(expr, decl, kind, false);
    }
  }
  
  private void checkAssignability(
      final IRNode expr, final IRNode decl, final LValue kind, final boolean noAnnoIsNonNull) {
    
    /*
     * Problem for results: if declPD is null, then we have something that is
     * @Nullable (or @NonNull) with no annotation. It is an error to pass a @Raw
     * reference to it, but then we do not have a promise to report the error
     * on. So we have to add a virtual @Nullable or @NonNull annotation. See the
     * ELSE branch.
     */
    final PromiseDrop<?> declPD = getAnnotationToAssure(decl);
    try {
      if (declPD != null && !createdVirtualAnnotations.contains(declPD)) { // Skip virtual @Nullables
        final StackQueryResult queryResult = currentQuery().getStackResult(expr);
        final Element declState = queryResult.getLattice().injectPromiseDrop(declPD);        
        final ResultsBuilder builder = new ResultsBuilder(declPD);
        ResultFolderDrop folder = builder.createRootAndFolder(expr,
            kind.getAssuredMessage(), kind.getFailedMessage(),
            declState.getAnnotation(), kind.getName(decl));
        buildNewChain(false, expr, folder, folder, kind, decl, declState, queryResult.getSources());
      } else {
        /*
         * Like above, but we know the declared state is implicitly @Nullable or
         * @NonNull, and we only care about the negative results. First we have to
         * determine if there are any negative results. If there are, we first
         * introduce a new NullablePromiseDrop or NonNullPromiseDrop.
         */
        final StackQueryResult queryResult = currentQuery().getStackResult(expr);
        final Element testAgainst = noAnnoIsNonNull ?
            NonNullRawLattice.NOT_NULL : NonNullRawLattice.MAYBE_NULL;
        final boolean hasNegativeResult = testChain(noAnnoIsNonNull, testAgainst, queryResult.getSources());
        if (hasNegativeResult) {
          // Do we already have a virtual promise drop?
          final PromiseDrop<?> drop;
          if (declPD == null) {
            if (noAnnoIsNonNull) {
              drop = NullableUtils.attachVirtualNonNull(decl, createdVirtualAnnotations);
            } else {
              drop = NullableUtils.attachVirtualNullable(decl, createdVirtualAnnotations);
            }
          } else {
            drop = declPD;
          }
          
          final ResultsBuilder builder = new ResultsBuilder(drop);
          ResultFolderDrop folder = builder.createRootAndFolder(expr,
              kind.getAssuredMessage(), kind.getFailedMessage(),
              testAgainst.getAnnotation(), kind.getName(decl));
          buildNewChain(noAnnoIsNonNull, expr, folder, folder, kind, decl, testAgainst, queryResult.getSources());
        }
      }
    } catch (AnalysisGaveUp e) {
      // do nothing
    }
  }

  private void buildNewChain(final boolean testRawOnly, 
      final IRNode rhsExpr, final ResultFolderDrop root, final AnalysisResultDrop parent,
      final LValue lvalue, final IRNode lhsDecl, final Element declState, final Set<Source> sources) {
    buildNewChain(testRawOnly, rhsExpr, root, parent, lvalue, lhsDecl, declState, sources,
        new HashMap<IRNode, AnalysisResultDrop>());
  }
  
  private static String getLocationString(final IJavaRef javaRef) {
    return javaRef.getEclipseProjectNameOrEmpty() + ":" +
        javaRef.getPackageName() + ":" +
        javaRef.getSimpleFileName() + ":" +
        javaRef.getOffset() +":" +
        javaRef.getLength();
  }
  
  private void buildNewChain(final boolean testRawOnly, 
      final IRNode rhsExpr, final ResultFolderDrop root, final AnalysisResultDrop parent,
      final LValue lvalue, final IRNode lhsDecl, final Element declState, final Set<Source> sources,
      final Map<IRNode, AnalysisResultDrop> visitedUseSites) {
    for (final Source src : sources) {
      final Kind k = src.first();
      final IRNode where = src.second();
        
      if (k == Kind.VAR_USE || k == Kind.THIS_EXPR) {
        final AnalysisResultDrop x = visitedUseSites.get(where);
        if (x != null) {
          parent.addTrusted(x);
        } else {
          final IRNode vd = k.bind(where, binder);
          final StackQueryResult newQuery = currentQuery().getStackResult(where);
          final Base varValue = newQuery.lookupVar(vd);
          final ResultFolderDrop f = ResultsBuilder.createAndFolder(
              parent, where, READ_FROM, READ_FROM, DebugUnparser.toString(where));
          final IKeyValue diffInfo = KeyValueUtility.getStringInstance(
              DiffHeuristics.ANALYSIS_DIFF_HINT,
              getLocationString(JavaNode.getJavaRef(rhsExpr)));
          f.addOrReplaceDiffInfo(diffInfo);

          visitedUseSites.put(where, f);
          buildNewChain(testRawOnly, rhsExpr, root, f, lvalue, lhsDecl, declState, varValue.second(), visitedUseSites);
        }
      } else if (k != Kind.NO_VALUE) {
        final Element srcState = src.third();
        final boolean rawSrc =
            srcState == NonNullRawLattice.RAW || srcState instanceof ClassElement;
        
        if (!testRawOnly || rawSrc) {
          final boolean isAssignableFrom =
              declState.isAssignableFrom(binder.getTypeEnvironment(), srcState);
          final ResultDrop result = ResultsBuilder.createResult(
              isAssignableFrom, parent, where,
              k.getMessage(), srcState.getAnnotation(), k.unparse(where));
          final IKeyValue diffInfo = KeyValueUtility.getStringInstance(
              DiffHeuristics.ANALYSIS_DIFF_HINT,
              getLocationString(JavaNode.getJavaRef(rhsExpr)));
          result.addOrReplaceDiffInfo(diffInfo);

          final PromiseDrop<?> pd = getAnnotationForProof(k.getAnnotatedNode(binder, where));
          if (pd != null) result.addTrusted(pd);
          
          /*
           * If the srcState is partially initialized and the lvalue is a 
           * method receiver, and the assignment FAILS, then we have to switch
           * the message on the root of the subchain so that the error message
           * better reflects the situation. 
           */
          if (rawSrc && lvalue == LValue.RECEIVER && !isAssignableFrom) {
            root.setMessageWhenNotProvedConsistent(
                UNACCEPTABLE_RAW_RECEIVER, declState.getAnnotation());
          }
          
          final Operator op = JJNode.tree.getOperator(rhsExpr);
          if (declState == NonNullRawLattice.MAYBE_NULL && rawSrc) {
            result.addInformationHint(where, RAW_INTO_NULLABLE);
          }
          
          /*
           * Propose @NonNull on a formal parameter if the formal is 
           * unannotated and the formal is being assigned to a @NonNull
           * field or actual.
           */
          if (declState == NonNullRawLattice.NOT_NULL && 
              VariableUseExpression.prototype.includes(op)) {
            final IRNode decl = binder.getBinding(rhsExpr);
            if (ParameterDeclaration.prototype.includes(decl) &&
                getAnnotationForProof(decl) == null) {
              result.addProposalNotProvedConsistent(new Builder(NonNull.class, decl, rhsExpr).build());
            }
          }
          
          /*
           * If the formal/receiver is unannotated and assigned an @Initialized
           * reference then we do a few special things:
           * 
           * (1) Propose an @Initialized annotation on it
           * 
           * (2) If the use is inside a constructor (but not a method),
           *     and the source is from the receiver, we add the 
           *     LHS promise under the classe's TrackPartiallyInitialized
           *     annotation (there must be one or else we would not have
           *     initialized references for the receiver).
           */
          
          /*
           * Propose @Initialized on a formal parameter or receiver
           * if the formal/receiver is unannotated and assigned an @Initialized
           * reference.
           * 
           * (NOTE: This is the reverse of what we do for @NonNull above)
           */
          final boolean notRawDecl =
              declState == NonNullRawLattice.NOT_NULL || 
              declState == NonNullRawLattice.MAYBE_NULL;
          if (notRawDecl && rawSrc &&
              ((ThisExpression.prototype.includes(op) ||
                  VariableUseExpression.prototype.includes(op)))) {
            final PromiseDrop<?> lhsPromise = getAnnotationForProof(lhsDecl);
            if ((ParameterDeclaration.prototype.includes(lhsDecl) || ReceiverDeclaration.prototype.includes(lhsDecl)) &&
                (lhsPromise == null || lhsPromise.isVirtual())) {
              makeInitializedProposal(result, parent, srcState, where, lhsDecl); 
            }
          }
          
          if (rawSrc && k == Kind.RECEIVER_CONSTRUCTOR_CALL) {
            final PromiseDrop<?> lhsPromise = getAnnotationForProof(lhsDecl);
            if ((ParameterDeclaration.prototype.includes(lhsDecl) || ReceiverDeclaration.prototype.includes(lhsDecl)) &&
                lhsPromise != null) {
              final TrackPartiallyInitializedPromiseDrop tpi = 
                  NonNullRules.getTrackPartiallyInitialized(VisitUtil.getEnclosingType(getEnclosingDecl()));
              tpi.addDependent(lhsPromise);
            }
          }
              
        }
      }
    }
  }

  private void makeInitializedProposal(
      final ResultDrop result, AnalysisResultDrop parent,
      final Element srcState, IRNode use, IRNode decl) {
	  if (srcState == NonNullRawLattice.RAW) {
	    result.addProposalNotProvedConsistent(
	        new Builder(Initialized.class, decl, use).build());
	  } else {
		  ClassElement ce = (ClassElement) srcState;
		  final IJavaDeclaredType through = ce.getType();
		  final IJavaType declaredType = binder.getJavaType(decl);
		  
		  /* It is possible to have a situation where the system needs to the type
		   * to be initialized through class T, but the declared type of the
		   * variable is S, where S is a supertype of T.  In this case, proposing
		   * that the variable to initialized through T is erroneous.  Also, the
		   * best way of dealing with this in the code is to use the special
		   * java.surelogic.Cast methods.
		   */
		  if (binder.getTypeEnvironment().isSubType(through, declaredType)) {
		    final IRNode exprToCast = parent.getNode();
        parent.addInformationHint(
            exprToCast, USE_CAST_NULLABLE, DebugUnparser.toString(exprToCast));
		  } else {
	      result.addProposalNotProvedConsistent(
  		      new Builder(Initialized.class, decl, use).addAttribute(
  		          AnnotationVisitor.THROUGH,
  		          SLUtility.unqualifyTypeNameInJavaLang(through.getName())).build());
		  }
	  }
  }

  private boolean testChain(
      final boolean testRawOnly, final Element declState, final Set<Source> sources) {
    return testChain(testRawOnly, declState, sources, new HashSet<IRNode>());
  }
  
  private boolean testChain(
      final boolean testRawOnly, final Element declState, final Set<Source> sources,
      final Set<IRNode> visitedUseSites) {
    boolean hasNegative = false;
    final Iterator<Source> it = sources.iterator();
    while (it.hasNext() && !hasNegative) {
      final Source src = it.next();
      final Kind k = src.first();
      final IRNode where = src.second();
        
      if (k == Kind.VAR_USE || k == Kind.THIS_EXPR) {
        if (!visitedUseSites.contains(where)) {
          visitedUseSites.add(where);
          final IRNode vd = k.bind(where, binder);
          final StackQueryResult newQuery = currentQuery().getStackResult(where);
          final Base varValue = newQuery.lookupVar(vd);
          hasNegative |= testChain(testRawOnly, declState, varValue.second(), visitedUseSites);
        }
      } else if (k != Kind.NO_VALUE) {
        final Element srcState = src.third();
        if (!testRawOnly || (srcState == NonNullRawLattice.RAW || srcState instanceof ClassElement)) {
          hasNegative |= !declState.isAssignableFrom(binder.getTypeEnvironment(), srcState);
        }
      }
    }
    return hasNegative;
  }

  
  
  @Override
  protected void checkUnboxExpression(
      final IRNode unboxExpr, final IRNode unboxedExpr) {
    checkForNull(unboxedExpr, true);
  }
  
  @Override
  protected void checkFieldInitialization(
      final IRNode fieldDecl, final IRNode varDecl) {
    final IRNode init = VariableDeclarator.getInit(varDecl);
    if (Initialization.prototype.includes(init)) {
      final IRNode initExpr = Initialization.getValue(init);
      final IRNode typeNode = VariableDeclarator.getType(varDecl);
      if (getAnnotationToAssure(varDecl) != null) {
        // Only check if the local is explicitly annotated
        checkAssignability(initExpr, varDecl, LValue.FIELD, typeNode);
      } else { // try to propose @NonNUll
        if (ReferenceType.prototype.includes(typeNode)) {
          recordFieldAssignment(varDecl, initExpr);
        }
      }
    }
  }
  
  @Override
  protected void checkReturnStatement(
      final IRNode returnStmt, final IRNode valueExpr) {
    // N.B. Must be a MethodDeclaration because constructors cannot return values
	if (LambdaExpression.prototype.includes(returnStmt)) {
		throw new UnsupportedOperationException();
	}
    final IRNode methodDecl = VisitUtil.getEnclosingMethod(returnStmt);
    final IRNode returnTypeNode = MethodDeclaration.getReturnType(methodDecl);
    checkAssignability(
        valueExpr, JavaPromise.getReturnNode(methodDecl), LValue.RETURN, returnTypeNode);
  }
  
  @Override
  protected void checkThrowStatement(
      final IRNode throwStmt, final IRNode thrownExpr) {
    checkForNull(thrownExpr);
  }
  
  @Override
  protected void checkSynchronizedStatement(
      final IRNode syncStmt, final IRNode lockExpr) {
    checkForNull(lockExpr);
  }
  
  @Override
  protected void checkOuterObjectSpecifier(final IRNode e,
      final IRNode object, final IRNode call) {
    checkForNull(object);
  }
  
  @Override
  protected void checkActualsVsFormals(final IRNode call,
      final IRNode actuals, final IRNode formals) {
    // Don't check the argument to the special cast methods
    if (NullableUtils.isCastMethod(binder.getBinding(call)) != null) {
      return;
    }
    
    // Actuals must be assignable to the formals
    final Iterator<IRNode> actualsIter = Arguments.getArgIterator(actuals);
    final Iterator<IRNode> formalsIter = Parameters.getFormalIterator(formals);
    while (actualsIter.hasNext()) {
      final IRNode actualExpr = actualsIter.next();
      final IRNode formalDecl = formalsIter.next();
      final IRNode formalTypeNode = ParameterDeclaration.getType(formalDecl);
      checkAssignability(actualExpr, formalDecl, LValue.PARAMETER, formalTypeNode);
    }    
  }

  @Override
  protected void checkMethodTarget(
      final IRNode call, final IRNode methodDecl, final IRNode target) {
    // (1) check that the target is not null
    checkForNull(target);
    
    // (2) check the target against the receiver annotation
    final IRNode rcvrDecl = JavaPromise.getReceiverNode(methodDecl);
    checkAssignability(target, rcvrDecl, LValue.RECEIVER, true);
  }
  
  @Override
  protected void checkFieldRef(
      final IRNode fieldRefExpr, final IRNode objectExpr) {
    final IRNode fieldDecl = binder.getBinding(fieldRefExpr);
    if (!TypeUtil.isStatic(fieldDecl)) {
      checkForNull(objectExpr);
    }
  }
  
  @Override
  protected void checkArrayRefExpression(final IRNode arrayRefExpr,
      final IRNode arrayExpr, final IRNode indexExpr) {
    checkForNull(arrayExpr);
  }
  
  @Override
  protected void checkAssignExpression(
      final IRNode assignExpr, final IRNode lhs, final IRNode rhs) {
    /* 
     * @NonNull fields must be assigned @NonNull references.
     * @NonNull locals must be assigned @NonNUll references.
     */
    final Operator op = JJNode.tree.getOperator(lhs);
    if (FieldRef.prototype.includes(op)) {
      final IRNode varDecl = binder.getBinding(lhs);
      final IRNode typeNode = VariableDeclarator.getType(varDecl);
      checkAssignability(rhs, varDecl, LValue.FIELD, typeNode);
      
      /* 
       * Track the assignments of unannotated deferred final fields for
       * annotation proposal purposes.
       * 
       * N.B. final fields may only be assigned to within a constructor, so we
       * can, in fact, find all assignments to the field no matter what the 
       * visiblity of the field.
       */
      if (//TypeUtil.isFinal(varDecl, false) &&
          ReferenceType.prototype.includes(typeNode) &&
          getAnnotationToAssure(varDecl) == null) {
        recordFieldAssignment(varDecl, rhs);
      }
    } else if (VariableUseExpression.prototype.includes(op)) {
      /* Only check the assignment if the lhs is local variable, NOT if it
       * is a parameter.  The annotations on the two mean different things.
       */
      final IRNode varOrParamDecl = binder.getBinding(lhs);
      if (VariableDeclarator.prototype.includes(varOrParamDecl)) {
        // Only check if the local is explicitly annotated
        if (getAnnotationToAssure(varOrParamDecl) != null) {
          final IRNode typeNode = VariableDeclarator.getType(varOrParamDecl);
          checkAssignability(rhs, varOrParamDecl, LValue.VARIABLE, typeNode);
        }
      }
    }
  }

  private void recordFieldAssignment(final IRNode varDecl, final IRNode rhs) {
    Element state = fieldInits.get(varDecl);
    Element rhsState = currentQuery().getStackResult(rhs).getValue();
    if (state == null) {
      fieldInits.put(varDecl, rhsState);
    } else {
      fieldInits.put(varDecl, state.join(rhsState));
    }
  }

  @Override
  protected void checkVariableInitialization(
      final IRNode declStmt, final IRNode vd) {
    final IRNode init = VariableDeclarator.getInit(vd);
    if (Initialization.prototype.includes(init)) {
      // Only check if the local is explicitly annotated
      if (getAnnotationToAssure(vd) != null) {
        final IRNode initExpr = Initialization.getValue(init);
        final IRNode typeNode = VariableDeclarator.getType(vd);
        checkAssignability(initExpr, vd, LValue.VARIABLE, typeNode);
      }
    }
  }
}
