package com.surelogic.analysis.nullable;

import com.surelogic.analysis.nullable.NonNullAnalysis.NullInfo;
import com.surelogic.analysis.nullable.RawLattice.Element;
import com.surelogic.analysis.type.checker.QualifiedTypeChecker;
import com.surelogic.dropsea.ir.HintDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.util.TypeUtil;

public final class NonNullTypeChecker extends QualifiedTypeChecker<NonNullTypeChecker.Queries> {
  private static final int POSSIBLY_NULL = 915;
  private static final int DEFINITELY_NULL = 916;
  
  
  
  private final IBinder binder;
  private final NonNullAnalysis nonNullAnalysis;
  private final RawTypeAnalysis rawTypeAnalysis;
  
  
  
  public NonNullTypeChecker(final IBinder b,
      final NonNullAnalysis nonNull, final RawTypeAnalysis raw) {
    binder = b;
    nonNullAnalysis = nonNull;
    rawTypeAnalysis = raw;
  }

  
  
  static final class Queries {
    private final NonNullAnalysis.StackQuery nonNull;
    private final RawTypeAnalysis.StackQuery rawType;
    
    public Queries(final IRNode flowUnit,
        final NonNullAnalysis nonNullAnalysis, 
        final RawTypeAnalysis rawTypeAnalysis) {
      nonNull = nonNullAnalysis.getStackQuery(flowUnit);
      rawType = rawTypeAnalysis.getStackQuery(flowUnit);
    }
    
    private Queries(final Queries q, final IRNode caller) {
      nonNull = q.nonNull.getSubAnalysisQuery(caller);
      rawType = q.rawType.getSubAnalysisQuery(caller);
    }
    
    public Queries getSubAnalysisQuery(final IRNode caller) {
      return new Queries(this, caller);
    }
    
    public NullInfo getNonNull(final IRNode node) {
      return nonNull.getResultFor(node);
    }
    
    public Element getRawType(final IRNode node) {
      return rawType.getResultFor(node);
    }
  }
  
  @Override
  protected Queries createNewQuery(final IRNode decl) {
    return new Queries(decl, nonNullAnalysis, rawTypeAnalysis);
  }

  @Override
  protected Queries createSubQuery(final IRNode caller) {
    return currentQuery().getSubAnalysisQuery(caller);
  }



  private void checkForNull(final IRNode expr) {
    /*
     * If the top of the stack is @NonNull or @Raw, then the value is definitely
     * not null.
     * 
     * So error state is when the top of the stack is not NOTNULL && is 
     * NOT_RAW
     */
    final NullInfo nullState = currentQuery().getNonNull(expr);
    final Element rawState = currentQuery().getRawType(expr);
    if (nullState != NullInfo.NOTNULL && rawState == RawLattice.NOT_RAW) {
      final HintDrop drop = HintDrop.newWarning(expr);
      drop.setMessage(nullState == NullInfo.MAYBENULL ? POSSIBLY_NULL : DEFINITELY_NULL);
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
//  
//  @Override
//  protected void checkArrayLength(
//      final IRNode arrayLenExpr, final IRNode objectExpr) {
//    checkForNull(objectExpr);
//  }
  
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
