package com.surelogic.analysis.nullable;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.surelogic.aast.*;
import com.surelogic.aast.promise.NonNullNode;
import com.surelogic.aast.promise.NullableNode;
import com.surelogic.analysis.AbstractThisExpressionBinder;
import com.surelogic.analysis.InstanceInitAction;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.nullable.NonNullRawLattice.ClassElement;
import com.surelogic.analysis.nullable.NonNullRawLattice.Element;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.Base;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.Kind;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.Source;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.StackQuery;
import com.surelogic.analysis.nullable.NonNullRawTypeAnalysis.StackQueryResult;
import com.surelogic.analysis.type.checker.QualifiedTypeChecker;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.common.concurrent.ConcurrentHashSet;
import com.surelogic.dropsea.IProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.AnalysisResultDrop;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NullablePromiseDrop;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.Arguments;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.ReferenceType;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public final class NonNullTypeChecker extends QualifiedTypeChecker<StackQuery> {
  private static final int POSSIBLY_NULL = 915;
  private static final int POSSIBLY_NULL_UNBOX = 916;
  private static final int READ_FROM = 917;
  
  private static final int GOOD_ASSIGN_FOLDER = 930;
  private static final int BAD_ASSIGN_FOLDER = 931;
  private static final int RAW_INTO_NULLABLE = 932;
  
  
  private final ThisExpressionBinder thisExprBinder;
  private final NonNullRawTypeAnalysis nonNullRawTypeAnalysis;

  private final Set<IRNode> badMethodBodies;
  
  private IRNode receiverDecl = null;

  private static final Set<PromiseDrop<?>> createdVirtualAnnotations = new ConcurrentHashSet<PromiseDrop<?>>();
  
  
  
  public NonNullTypeChecker(final IBinder b,
      final NonNullRawTypeAnalysis nonNullRaw,
      final Set<IRNode> badMethodBodies) {
    super(b);
    thisExprBinder = new ThisExpressionBinder(b);
    nonNullRawTypeAnalysis = nonNullRaw;
    this.badMethodBodies = badMethodBodies;
  }
  
  public static void clearCaches() {
    createdVirtualAnnotations.clear();
  }
  
  
  
  // ======================================================================
  // == Manage the binding of this expression
  // ======================================================================
  
  @Override
  protected void handleMethodDeclaration(final IRNode mdecl) {
    final IRNode oldReceiverDecl = receiverDecl;
    try {
      receiverDecl = JavaPromise.getReceiverNodeOrNull(mdecl);
      System.out.println("NonNull: " + JavaNames.genQualifiedMethodConstructorName(mdecl));
      super.handleMethodDeclaration(mdecl);
    } finally {
      receiverDecl = oldReceiverDecl;
    }
  }
  
  @Override
  protected void handleConstructorDeclaration(final IRNode cdecl) {
    final IRNode oldReceiverDecl = receiverDecl;
    try {
      receiverDecl = JavaPromise.getReceiverNodeOrNull(cdecl);
      System.out.println("NonNull: " + JavaNames.genQualifiedMethodConstructorName(cdecl));
      super.handleConstructorDeclaration(cdecl);
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
  protected StackQuery createNewQuery(final IRNode decl) {
    return nonNullRawTypeAnalysis.getStackQuery(decl);
  }

  @Override
  protected StackQuery createSubQuery(final IRNode caller) {
    return currentQuery().getSubAnalysisQuery(caller);
  }



  private void checkForNull(final IRNode expr) {
    checkForNull(expr, false);
  }
  
  private void checkForNull(final IRNode expr, final boolean isUnbox) {
    final StackQueryResult queryResult = currentQuery().getResultFor(expr);
    final Element state = queryResult.getValue();    
    if (state == NonNullRawLattice.MAYBE_NULL || state == NonNullRawLattice.NULL) {
      // Hunt for any @Nullable annotations 
      buildWarningResults(isUnbox, expr, 
          queryResult.getSources(), new LinkedList<IRNode>());
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
        final IRNode vd = k.bind(where, binder, thisExprBinder);
        final StackQueryResult newQuery = currentQuery().getResultFor(where);
        final Base varValue = newQuery.lookupVar(vd);
        chain.addLast(where);
        buildWarningResults(isUnbox, expr, varValue.second(), chain);
        chain.removeLast();    
      } else {
        final PromiseDrop<?> pd = getAnnotation(k.getAnnotatedNode(binder, where));
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
        }
      }
    }
  }
  
  private PromiseDrop<?> getAnnotation(final IRNode n) {
    PromiseDrop<?> pd = NonNullRules.getRaw(n);
    if (pd == null) pd = NonNullRules.getNonNull(n);
    if (pd == null) pd = NonNullRules.getNullable(n);
    return pd;
  }
  
  private void checkAssignability(
      final IRNode expr, final IRNode decl, final IRNode declTypeNode) {
    if (ReferenceType.prototype.includes(declTypeNode)) {
      checkAssignability(expr, decl, false);
    }
  }
  
  private void checkAssignability(
      final IRNode expr, final IRNode decl, final boolean noAnnoIsNonNull) {
    /*
     * Problem for results: if declPD is null, then we have something that is
     * @Nullable (or @NonNull) with no annotation. It is an error to pass a @Raw
     * reference to it, but then we do not have a promise to report the error
     * on. So we have to add a virtual @Nullable or @NonNull annotation. See the
     * ELSE branch.
     */
    final PromiseDrop<?> declPD = getAnnotation(decl);
    if (declPD != null && !createdVirtualAnnotations.contains(declPD)) { // Skip virtual @Nullables
      final StackQueryResult queryResult = currentQuery().getResultFor(expr);
      final Element declState = queryResult.getLattice().injectPromiseDrop(declPD);        
      final ResultsBuilder builder = new ResultsBuilder(declPD);
      ResultFolderDrop folder = builder.createRootAndFolder(
          expr, GOOD_ASSIGN_FOLDER, BAD_ASSIGN_FOLDER,
          declState.getAnnotation());
      buildNewChain(false, expr, folder, declState, queryResult.getSources());
    } else {
      /*
       * Like above, but we know the declared state is implicitly @Nullable or
       * @NonNull, and we only care about the negative results. First we have to
       * determine if there are any negative results. If there are, we first
       * introduce a new NullablePromiseDrop or NonNullPromiseDrop.
       */
      final StackQueryResult queryResult = currentQuery().getResultFor(expr);
      final Element testAgainst = noAnnoIsNonNull ?
          NonNullRawLattice.NOT_NULL : NonNullRawLattice.MAYBE_NULL;
      final boolean hasNegativeResult = testChain(noAnnoIsNonNull, testAgainst, queryResult.getSources());
      if (hasNegativeResult) {
        // Do we already have a virtual promise drop?
        final PromiseDrop<?> drop;
        if (declPD == null) {
          if (noAnnoIsNonNull) {
            final NonNullNode nn = new NonNullNode(0);
            nn.setPromisedFor(decl, null);
            drop = attachAsVirtual(NonNullRules.getNonNullStorage(), new NonNullPromiseDrop(nn));
          } else {
            final NullableNode nn = new NullableNode(0);
            nn.setPromisedFor(decl, null);
            drop = attachAsVirtual(NonNullRules.getNullableStorage(), new NullablePromiseDrop(nn));
          }
        } else {
          drop = declPD;
        }
        
        final ResultsBuilder builder = new ResultsBuilder(drop);
        ResultFolderDrop folder = builder.createRootAndFolder(
            expr, GOOD_ASSIGN_FOLDER, BAD_ASSIGN_FOLDER,
            testAgainst.getAnnotation());
        buildNewChain(noAnnoIsNonNull, expr, folder, testAgainst, queryResult.getSources());
      }
    }
  }

  private <A extends IAASTRootNode, T extends PromiseDrop<? super A>> T attachAsVirtual(IPromiseDropStorage<T> storage, T drop) {
	  try {
	     AnnotationRules.attachAsVirtual(storage, drop);
         createdVirtualAnnotations.add(drop);
         return drop;
	  } catch(IllegalArgumentException e) {
		 // Assumed to be already created
		 drop.invalidate();
		 return drop.getNode().getSlotValue(storage.getSlotInfo());
		 //return storage.getDrops(drop.getNode()).iterator().next();
	  }
  }

  private void buildNewChain(final boolean testRawOnly, 
      final IRNode rhsExpr, final AnalysisResultDrop parent,
      final Element declState, final Set<Source> sources) {
    buildNewChain(testRawOnly, rhsExpr, parent, declState, sources,
        new HashMap<IRNode, AnalysisResultDrop>());
  }
  
  private void buildNewChain(final boolean testRawOnly, 
      final IRNode rhsExpr, final AnalysisResultDrop parent,
      final Element declState, final Set<Source> sources,
      final Map<IRNode, AnalysisResultDrop> visitedUseSites) {
    for (final Source src : sources) {
      final Kind k = src.first();
      final IRNode where = src.second();
        
      if (k == Kind.VAR_USE || k == Kind.THIS_EXPR) {
        final AnalysisResultDrop x = visitedUseSites.get(where);
        if (x != null) {
          parent.addTrusted(x);
        } else {
          final IRNode vd = k.bind(where, binder, thisExprBinder);
          final StackQueryResult newQuery = currentQuery().getResultFor(where);
          final Base varValue = newQuery.lookupVar(vd);
          final ResultFolderDrop f = ResultsBuilder.createAndFolder(
              parent, where, READ_FROM, READ_FROM, DebugUnparser.toString(where));
          visitedUseSites.put(where, f);
          buildNewChain(testRawOnly, rhsExpr, f, declState, varValue.second(), visitedUseSites);
        }
      } else {
        final Element srcState = src.third();
        if (!testRawOnly || (srcState == NonNullRawLattice.RAW || srcState instanceof ClassElement)) {
          final ResultDrop result = ResultsBuilder.createResult(
              declState.isAssignableFrom(binder.getTypeEnvironment(), srcState),
              parent, where,
              k.getMessage(), srcState.getAnnotation(), k.unparse(where));
          final PromiseDrop<?> pd = getAnnotation(k.getAnnotatedNode(binder, where));
          if (pd != null) result.addTrusted(pd);
          
          if (declState == NonNullRawLattice.MAYBE_NULL &&
              (srcState == NonNullRawLattice.RAW ||
               srcState instanceof ClassElement)) {
            result.addInformationHint(where, RAW_INTO_NULLABLE);
          }
          
          if (declState == NonNullRawLattice.NOT_NULL && 
              VariableUseExpression.prototype.includes(rhsExpr)) {
            final IRNode decl = binder.getBinding(rhsExpr);
            if (ParameterDeclaration.prototype.includes(decl) &&
                getAnnotation(decl) == null) {
              result.addProposalNotProvedConsistent(
                  new ProposedPromiseDrop(
                      "NonNull", null, decl, rhsExpr, Origin.PROBLEM));
            }
          }
        }
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
          final IRNode vd = k.bind(where, binder, thisExprBinder);
          final StackQueryResult newQuery = currentQuery().getResultFor(where);
          final Base varValue = newQuery.lookupVar(vd);
          hasNegative |= testChain(testRawOnly, declState, varValue.second(), visitedUseSites);
        }
      } else {
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
    checkAssignmentInitializer(varDecl);
  }
  
  @Override
  protected void checkReturnStatement(
      final IRNode returnStmt, final IRNode valueExpr) {
    // N.B. Must be a MethodDeclaration because constructors cannot return values
    final IRNode methodDecl = VisitUtil.getEnclosingMethod(returnStmt);
    final IRNode returnTypeNode = MethodDeclaration.getReturnType(methodDecl);
    checkAssignability(
        valueExpr, JavaPromise.getReturnNode(methodDecl), returnTypeNode);
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
    // Actuals must be assignable to the formals
    final Iterator<IRNode> actualsIter = Arguments.getArgIterator(actuals);
    final Iterator<IRNode> formalsIter = Parameters.getFormalIterator(formals);
    while (actualsIter.hasNext()) {
      final IRNode actualExpr = actualsIter.next();
      final IRNode formalDecl = formalsIter.next();
      final IRNode formalTypeNode = ParameterDeclaration.getType(formalDecl);
      checkAssignability(actualExpr, formalDecl, formalTypeNode);
    }    
  }

  @Override
  protected void checkMethodTarget(
      final IRNode call, final IRNode methodDecl, final IRNode target) {
    // (1) check that the target is not null
    checkForNull(target);
    
    // (2) check the target against the receiver annotation
    final IRNode rcvrDecl = JavaPromise.getReceiverNode(methodDecl);
    checkAssignability(target, rcvrDecl, true);
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
      checkAssignability(rhs, varDecl, typeNode);
    } else if (VariableUseExpression.prototype.includes(op)) {
      /* Only check the assignment if the lhs is local variable, NOT if it
       * is a parameter.  The annotations on the two mean different things.
       */
      final IRNode varOrParamDecl = binder.getBinding(lhs);
      if (VariableDeclarator.prototype.includes(varOrParamDecl)) {
        // Only check if the local is explicitly annotated
        if (getAnnotation(varOrParamDecl) != null) {
          final IRNode typeNode = VariableDeclarator.getType(varOrParamDecl);
          checkAssignability(rhs, varOrParamDecl, typeNode);
        }
      }
    }
  }

  @Override
  protected void checkVariableInitialization(
      final IRNode declStmt, final IRNode vd) {
    checkAssignmentInitializer(vd);
  }

  private void checkAssignmentInitializer(final IRNode vd) {
    final IRNode init = VariableDeclarator.getInit(vd);
    if (Initialization.prototype.includes(init)) {
      // Only check if the local is explicitly annotated
      if (getAnnotation(vd) != null) {
        final IRNode initExpr = Initialization.getValue(init);
        final IRNode typeNode = VariableDeclarator.getType(vd);
        checkAssignability(initExpr, vd, typeNode);
      }
    }
  }
}
