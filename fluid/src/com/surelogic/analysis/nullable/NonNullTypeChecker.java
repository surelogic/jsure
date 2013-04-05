package com.surelogic.analysis.nullable;

import com.surelogic.analysis.nullable.combined.NonNullRawLattice;
import com.surelogic.analysis.nullable.combined.NonNullRawLattice.Element;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis;
import com.surelogic.analysis.nullable.combined.NonNullRawTypeAnalysis.StackQuery;
import com.surelogic.analysis.type.checker.QualifiedTypeChecker;
import com.surelogic.dropsea.ir.HintDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.util.TypeUtil;

public final class NonNullTypeChecker extends QualifiedTypeChecker<StackQuery> {
  private static final int POSSIBLY_NULL = 915;
  private static final int DEFINITELY_NULL = 916;
  
  
  
  private final IBinder binder;
//  private final NonNullAnalysis nonNullAnalysis;
//  private final RawTypeAnalysis rawTypeAnalysis;
  private final NonNullRawTypeAnalysis nonNullRawTypeAnalysis;
  
  
  
  public NonNullTypeChecker(final IBinder b,
      final NonNullRawTypeAnalysis nonNullRaw) {
    binder = b;
    nonNullRawTypeAnalysis = nonNullRaw;
  }

  
  
//  static final class Queries {
//    private final NonNullAnalysis.StackQuery nonNull;
//    private final RawTypeAnalysis.StackQuery rawType;
//    
//    public Queries(final IRNode flowUnit,
//        final NonNullAnalysis nonNullAnalysis, 
//        final RawTypeAnalysis rawTypeAnalysis) {
//      nonNull = nonNullAnalysis.getStackQuery(flowUnit);
//      rawType = rawTypeAnalysis.getStackQuery(flowUnit);
//    }
//    
//    private Queries(final Queries q, final IRNode caller) {
//      nonNull = q.nonNull.getSubAnalysisQuery(caller);
//      rawType = q.rawType.getSubAnalysisQuery(caller);
//    }
//    
//    public Queries getSubAnalysisQuery(final IRNode caller) {
//      return new Queries(this, caller);
//    }
//    
//    public NullInfo getNonNull(final IRNode node) {
//      return nonNull.getResultFor(node);
//    }
//    
//    public Element getRawType(final IRNode node) {
//      return rawType.getResultFor(node);
//    }
//  }
  
  @Override
  protected StackQuery createNewQuery(final IRNode decl) {
    return nonNullRawTypeAnalysis.getStackQuery(decl);
  }

  @Override
  protected StackQuery createSubQuery(final IRNode caller) {
    return currentQuery().getSubAnalysisQuery(caller);
  }



  private void checkForNull(final IRNode expr) {
    final Element state = currentQuery().getResultFor(expr);
    if (state == NonNullRawLattice.MAYBE_NULL) {
      final HintDrop drop = HintDrop.newWarning(expr);
      drop.setMessage(POSSIBLY_NULL);
    } else if (state == NonNullRawLattice.NULL) {
      final HintDrop drop = HintDrop.newWarning(expr);
      drop.setMessage(DEFINITELY_NULL);
    }
  }

  
  
  @Override
  protected void checkUnboxExpression(
      final IRNode unboxExpr, final IRNode unboxedExpr) {
    checkForNull(unboxedExpr);
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
}
