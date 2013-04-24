package com.surelogic.analysis.nullable;

import java.util.Iterator;

import com.surelogic.analysis.nullable.combined.NonNullRawLattice;
import com.surelogic.analysis.nullable.combined.NonNullRawLattice.Element;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.StackQuery;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.StackQueryResult;
import com.surelogic.analysis.type.checker.QualifiedTypeChecker;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.RawPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
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
  private static final int DEFINITELY_NULL = 916;
  private static final int POSSIBLY_NULL_UNBOX = 917;
  private static final int DEFINITELY_NULL_UNBOX = 918; 
  private static final int ASSIGNABLE = 919;
  private static final int NOT_ASSIGNABLE = 920;
  
  
  
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
    final Element state = currentQuery().getResultFor(expr).getValue();
    if (state == NonNullRawLattice.MAYBE_NULL) {
      final HintDrop drop = HintDrop.newWarning(expr);
      drop.setMessage(isUnbox ? POSSIBLY_NULL_UNBOX : POSSIBLY_NULL);
    } else if (state == NonNullRawLattice.NULL) {
      final HintDrop drop = HintDrop.newWarning(expr);
      drop.setMessage(isUnbox ? DEFINITELY_NULL_UNBOX : DEFINITELY_NULL);
    }
  }
  
  private String getTypeName(final IJavaType type, final Element qualifier) {
    if (qualifier == NonNullRawLattice.NULL) {
      return "null";
    } else {
      return qualifier.getAnnotation() + " " + type.toSourceText();
    }
  }

  private void checkReferenceAssignability(
      final IRNode expr, 
      final IJavaType type, final Element state,
      final IJavaType potentialAncestorType,
      final Element potentialAncestorState) {
    /*
     * Raw types cannot be given to MAYBE_NULL because they might not be
     * fully initialized.  The whole point of raw types is to keep track
     * of how much of the object has been initialized so far.
     * 
     * We do not allow @Raw on array types.  There is no way to view
     * an array that is partially initialized.
     * 
     * We don't actually need to test if the types are subtypes because
     * we are assuming the normal Java code compiles/type-checks cleanly.
     */
    final boolean isGood = potentialAncestorState.isAssignableFrom(
        binder.getTypeEnvironment(), state);
    final HintDrop drop = HintDrop.newWarning(expr);
    drop.setMessage(isGood ? ASSIGNABLE : NOT_ASSIGNABLE,
        getTypeName(type, state),
        getTypeName(potentialAncestorType, potentialAncestorState));

//    ResultsBuilder.createResult(expr, pd, isGood, ASSIGNABLE, NOT_ASSIGNABLE,
//        state, type.toSourceText(),
//        potentialAncestorState, potentialAncestorType.toSourceText());
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
      final IJavaType declType =
          JavaTypeFactory.convertNodeTypeToIJavaType(declTypeNode, binder);
      checkAssignability(expr, decl, declType, false);
    }
  }
  
  private void checkAssignability(
      final IRNode expr, final IRNode decl, final IJavaType declType,
      final boolean onlyCheckIfRaw) {
    final IJavaType exprType = binder.getJavaType(expr);
    final StackQueryResult queryResult = currentQuery().getResultFor(expr);
    final Element exprState = queryResult.getValue();
    
    final PromiseDrop<?> declPD = getPromise(decl);
    if (!onlyCheckIfRaw || declPD instanceof RawPromiseDrop) {
      final Element declState = queryResult.getLattice().injectPromiseDrop(declPD);
      checkReferenceAssignability(expr, exprType, exprState, declType, declState);
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
    final IRNode typeDecl = VisitUtil.getEnclosingType(methodDecl);
    final IJavaType type = JavaTypeFactory.getMyThisType(typeDecl);
    checkAssignability(target, rcvrDecl, type, true);
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
