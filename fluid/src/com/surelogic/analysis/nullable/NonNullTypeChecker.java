package com.surelogic.analysis.nullable;

import com.surelogic.analysis.nullable.NonNullAnalysis.NullInfo;
import com.surelogic.analysis.nullable.NonNullAnalysis.StackQuery;
import com.surelogic.analysis.type.checker.QualifiedTypeChecker;
import com.surelogic.dropsea.ir.HintDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public final class NonNullTypeChecker extends QualifiedTypeChecker<StackQuery> {
  private static final int POSSIBLY_NULL = 915;
  private static final int DEFINITELY_NULL = 916;
  
  
  
  private final NonNullAnalysis nonNullAnalysis;
  
  
  
  public NonNullTypeChecker(final NonNullAnalysis nonNull) {
    nonNullAnalysis = nonNull;
  }

  
  
  @Override
  protected StackQuery createNewQuery(final IRNode decl) {
    return nonNullAnalysis.getStackQuery(decl);
  }

  @Override
  protected StackQuery createSubQuery(final IRNode caller) {
    return currentQuery().getSubAnalysisQuery(caller);
  }



  @SuppressWarnings("unused")
  private void checkForNull(final IRNode expr) {
    final NullInfo nullState = currentQuery().getResultFor(expr);
    final Operator op = JJNode.tree.getOperator(expr);
    final String s = DebugUnparser.toString(expr);
    if (nullState != NullInfo.NOTNULL) {
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
}
