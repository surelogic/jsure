package com.surelogic.analysis.nullable;

import com.surelogic.analysis.nullable.combined.NonNullRawLattice;
import com.surelogic.analysis.nullable.combined.NonNullRawLattice.ClassElement;
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
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ReferenceType;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public final class NonNullTypeChecker extends QualifiedTypeChecker<StackQuery> {
  private static final int POSSIBLY_NULL = 915;
  private static final int DEFINITELY_NULL = 916;
  private static final int POSSIBLY_NULL_UNBOX = 917;
  private static final int DEFINITELY_NULL_UNBOX = 918; 
  private static final int ASSIGNABLE = 919;
  private static final int NOT_ASSIGNABLE = 920;
  
  
  
  private final IBinder binder;
  private final NonNullRawTypeAnalysis nonNullRawTypeAnalysis;
  
  
  public NonNullTypeChecker(final IBinder b,
      final NonNullRawTypeAnalysis nonNullRaw) {
    binder = b;
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
  
  private boolean isSubType(final IJavaType type, final Element state,
      final IJavaType potentialAncestorType,
      final Element potentialAncestorState) {
    final ITypeEnvironment typeEnvironment = binder.getTypeEnvironment();
    /*
     * Raw types cannot be given to MAYBE_NULL because they might not be
     * fully initialized.  The whole point of raw types is to keep track
     * of how much of the object has been initialized so far.
     * 
     * We do not allow @Raw on array types.  There is no way to view
     * an array that is partially initialized.
     */

    // If neither type is qualified, do the normal thing
    if (state == NonNullRawLattice.MAYBE_NULL &&
        potentialAncestorState == NonNullRawLattice.MAYBE_NULL) {
      return typeEnvironment.isSubType(type, potentialAncestorType);
    } else {
      // At least one of the types is qualified
      
//      // Are both types arrays?
//      if (type instanceof IJavaArrayType && 
//          potentialAncestorType instanceof IJavaArrayType) {
//        final IJavaType baseType = ((IJavaArrayType) type).getBaseType();
//        final IJavaType potentialAncestorBaseType =
//            ((IJavaArrayType) potentialAncestorType).getBaseType();
//        // Dead in the water if normal subtyping fails
//        if (typeEnvironment.isSubType(type, potentialAncestorType)) {
//        }
//        
//        // TODO
//        return false;
//      } else {
        // At least one of the types is qualified, and neither is an array
        
        // Dead in the water if normal subtyping fails
        if (typeEnvironment.isSubType(type, potentialAncestorType)) {
          if (potentialAncestorState == NonNullRawLattice.MAYBE_NULL) {
            return state == NonNullRawLattice.NOT_NULL ||
                state == NonNullRawLattice.MAYBE_NULL ||
                state == NonNullRawLattice.NULL;
          } else if (potentialAncestorState == NonNullRawLattice.NOT_NULL) {
            return state == NonNullRawLattice.NOT_NULL;
          } else if (potentialAncestorState == NonNullRawLattice.RAW) {
            return state == NonNullRawLattice.NOT_NULL ||
                state == NonNullRawLattice.RAW ||
                state instanceof ClassElement;
          } else if (potentialAncestorState instanceof ClassElement) {
            if (state instanceof ClassElement) {
              final IJavaType t1 = ((ClassElement) state).getType();
              final IJavaType t2 = ((ClassElement) potentialAncestorState).getType();
              return typeEnvironment.isSubType(t1, t2);
            } else {
              return state == NonNullRawLattice.NOT_NULL;
            }
          } else {
            return false;
          }
        } else {
          return false;
        }
//      }
    }
  }
  
  private String getTypeName(final IJavaType type, final Element qualifier) {
    if (qualifier == NonNullRawLattice.NULL) {
      return "null";
    } else {
      return qualifier.getAnnotation() + " " + type.toSourceText();
    }
  }

  private void checkAssignability(
      final IRNode expr, 
      final IJavaType type, final Element state,
      final IJavaType potentialAncestorType,
      final Element potentialAncestorState) {
    final boolean isGood =
        isSubType(type, state, potentialAncestorType, potentialAncestorState);
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

  
  
  @Override
  protected void checkUnboxExpression(
      final IRNode unboxExpr, final IRNode unboxedExpr) {
    checkForNull(unboxedExpr, true);
  }
  
  @Override
  protected void checkReturnStatement(
      final IRNode returnStmt, final IRNode valueExpr) {
    // N.B. Must be a MethodDeclaration because constructors cannot return values
    final IRNode methodDecl = VisitUtil.getEnclosingMethod(returnStmt);
    final IRNode returnTypeNode = MethodDeclaration.getReturnType(methodDecl);
    if (ReferenceType.prototype.includes(returnTypeNode)) {
      final IJavaType exprType = binder.getJavaType(valueExpr);
      final StackQueryResult queryResult = currentQuery().getResultFor(valueExpr);
      final Element exprState = queryResult.getValue();

      final IJavaType returnType =
          JavaTypeFactory.convertNodeTypeToIJavaType(returnTypeNode, binder);
      final PromiseDrop<?> returnPD =
          getPromise(JavaPromise.getReturnNode(methodDecl));
      final Element returnState = 
              queryResult.getLattice().injectPromiseDrop(returnPD);
      
      checkAssignability(
          valueExpr, exprType, exprState, returnType, returnState);
    }
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
    // TODO: Need to check for assignment compatibility
  }
}
