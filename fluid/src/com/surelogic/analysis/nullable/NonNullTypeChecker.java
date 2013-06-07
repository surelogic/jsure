package com.surelogic.analysis.nullable;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.nullable.combined.NonNullRawLattice;
import com.surelogic.analysis.nullable.combined.NonNullRawLattice.Element;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.Base;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.Kind;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.SimpleKind;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.Source;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.StackQuery;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.StackQueryResult;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.ThisKind;
import com.surelogic.analysis.type.checker.QualifiedTypeChecker;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NullablePromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.RawPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
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
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public final class NonNullTypeChecker extends QualifiedTypeChecker<StackQuery> {
  private static final int POSSIBLY_NULL = 915;
  private static final int POSSIBLY_NULL_UNBOX = 916;
  private static final int READ_FROM = 917;
  
  private static final int GOOD_ASSIGN_FOLDER = 930;
  private static final int BAD_ASSIGN_FOLDER = 931;
  private static final int GOOD_ASSIGN = 932;
  private static final int BAD_ASSIGN = 933;
  
  
  
  private final NonNullRawTypeAnalysis nonNullRawTypeAnalysis;
  
  
  public NonNullTypeChecker(final IBinder b,
      final NonNullRawTypeAnalysis nonNullRaw) {
    super(b);
    nonNullRawTypeAnalysis = nonNullRaw;
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
      buildWarningResults(isUnbox, expr, queryResult,
          queryResult.getSources(), new LinkedList<IRNode>());
    }
  }
  
  private void buildWarningResults(
      final boolean isUnbox,
      final IRNode expr,
      final StackQueryResult queryResult,
      final Set<Source> sources,
      final Deque<IRNode> chain) {
    // Hunt for any @Nullable annotations 
    for (final Source src : sources) {
      final Kind k = src.first();
      final IRNode where = src.second();
      
      if (k == SimpleKind.VAR_USE || k instanceof ThisKind) {
        final IRNode vd = binder.getBinding(where);
        final Base varValue = queryResult.lookupVar(vd);
        chain.addLast(where);
        buildWarningResults(isUnbox, expr, queryResult, varValue.second(), chain);
        chain.removeLast();    
      } else {
        final PromiseDrop<?> pd = getAnnotation(k.getAnnotatedNode(binder, where));
        if (pd instanceof NullablePromiseDrop) { // N.B. null is never an instance of anything
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
  
  private PromiseDrop<?> getPromise(final IRNode node) {
    final NonNullPromiseDrop nonNull = NonNullRules.getNonNull(node);
    if (nonNull != null) return nonNull;
    final RawPromiseDrop raw = NonNullRules.getRaw(node);
    return raw;
  }
  
  private void checkAssignability(
      final IRNode expr, final IRNode decl, final IRNode declTypeNode) {
    if (ReferenceType.prototype.includes(declTypeNode)) {
      checkAssignability(expr, decl, false);
    }
  }
  
  private void checkAssignability(
      final IRNode expr, final IRNode decl, final boolean onlyCheckIfRaw) {
    final PromiseDrop<?> declPD = getPromise(decl);
    if (!onlyCheckIfRaw || declPD instanceof RawPromiseDrop) {
      /* XXX: Problem for results: if declPD is null, then we have something
       * that is @Nullable with no annotation.  It is an error to pass a @Raw
       * reference to it, but then we do not have a promise to report the error
       * on. 
       * 
       * Possible solution: report the error on both the LHS promise, and the
       * RHS promise?  Try this later.
       */
      if (declPD != null) {
        final StackQueryResult queryResult = currentQuery().getResultFor(expr);
        final Element declState = queryResult.getLattice().injectPromiseDrop(declPD);        
        final ResultsBuilder builder = new ResultsBuilder(declPD);
        ResultFolderDrop folder = builder.createRootAndFolder(
            expr, GOOD_ASSIGN_FOLDER, BAD_ASSIGN_FOLDER,
            declState.getAnnotation());
        for (final Source src : queryResult.getSources()) {
          buildChain(folder, expr, declState, queryResult, src, new LinkedList<IRNode>());
        }
      }
    }
  }
  
  private void buildChain(
      final ResultFolderDrop folder, final IRNode origExpr, final Element declState,
      final StackQueryResult queryResult, final Source src,
      final Deque<IRNode> chain) {
    final Kind k = src.first();
    final IRNode where = src.second();
      
    if (k == SimpleKind.VAR_USE || k instanceof ThisKind) {
      final IRNode vd = binder.getBinding(where);
      final Base varValue = queryResult.lookupVar(vd);
      chain.addLast(where);
      for (final Source src2 : varValue.second()) {
        final StackQueryResult newQuery = currentQuery().getResultFor(src2.second());
        buildChain(folder, origExpr, declState, newQuery, src2, chain);
      }
      chain.removeLast();
    } else {
      final Element srcState = src.third();
      final ResultDrop result = ResultsBuilder.createResult(
          folder, origExpr,
          declState.isAssignableFrom(binder.getTypeEnvironment(), srcState),
          GOOD_ASSIGN, BAD_ASSIGN,
          srcState.getAnnotation(), declState.getAnnotation());

      final PromiseDrop<?> pd = getAnnotation(k.getAnnotatedNode(binder, where));
      if (pd != null) {
        result.addTrusted(pd);
      }

      Drop hd = result;
      for (final IRNode readFrom : chain) {
        hd = hd.addInformationHint(
            readFrom, READ_FROM, DebugUnparser.toString(readFrom));
      }  
      hd.addInformationHint(
          where, k.getMessage(),
          srcState.getAnnotation(), k.unparse(where));
    }
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
      checkAssignability(initExpr, varDecl, typeNode);
    }
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
     */
    if (FieldRef.prototype.includes(lhs)) {
      final IRNode fieldDecl = binder.getBinding(lhs);
      final IRNode typeNode = VariableDeclarator.getType(fieldDecl);
      checkAssignability(rhs, fieldDecl, typeNode);
    }
  }
}
