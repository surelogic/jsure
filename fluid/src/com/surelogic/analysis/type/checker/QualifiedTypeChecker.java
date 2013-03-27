package com.surelogic.analysis.type.checker;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.BoxExpression;
import edu.cmu.cs.fluid.java.operator.SynchronizedStatement;
import edu.cmu.cs.fluid.java.operator.ThrowStatement;
import edu.cmu.cs.fluid.java.operator.UnboxExpression;

/**
 * Visitor that tries to provide method hooks for semantic situations that 
 * might be interesting when verifying type qualifiers.  This situations are 
 * based on those places where the JLS indicates a statement or expression
 * may generate a particular run-time exception.
 */
public abstract class QualifiedTypeChecker<Q> extends AbstractJavaAnalysisDriver<Q> {
  protected QualifiedTypeChecker() {
    // TODO Auto-generated constructor stub
  }

  
  
  // ======================================================================
  // == Conversions
  // ======================================================================

  @Override
  public final Void visitBoxExpression(final IRNode e) {
    /*
     * ¤5.1.7
     * 
     * A boxing conversion may result in an OutOfMemoryError if a new instance
     * of one of the wrapper classes (Boolean, Byte, Character, Short, Integer,
     * Long, Float, or Double) needs to be allocated and insufficient storage is
     * available.
     */
    doAcceptForChildren(e);
    checkBoxExpression(e, BoxExpression.getOp(e));
    return null;
  }
  
  protected void checkBoxExpression(
      final IRNode boxExpr, final IRNode boxedExpr) {
    // Do nothing
  }
  
  
  
  @Override
  public final Void visitUnboxExpression(final IRNode e) {
    /*
     * ¤5.1.8
     * 
     * At run-time, unboxing conversion proceeds as follows: É
     * 
     * If r is null, unboxing conversion throws a NullPointerException
     */
    doAcceptForChildren(e);
    checkUnboxExpression(e, UnboxExpression.getOp(e));
    return null;
  }
  
  protected void checkUnboxExpression(
      final IRNode unboxExpr, final IRNode unboxedExpr) {
    // Do nothing
  }

  
  
  // ======================================================================
  // == Statements
  // ======================================================================

  // TODO: Return statements: needs to be treated like an assignment
  
  @Override
  public final Void visitThrowStatement(final IRNode s) {
    /*
     * ¤14.18
     * 
     * A throw statement first evaluates the Expression. Then:
     * 
     * If evaluation of the Expression completes normally, producing a null
     * value, then an instance V' of class NullPointerException is created and
     * thrown instead of null. The throw statement then completes abruptly, the
     * reason being a throw with value V'.
     */
    doAcceptForChildren(s);
    checkThrowStatement(s, ThrowStatement.getValue(s));
    return null;
  }
  
  protected void checkThrowStatement(
      final IRNode throwStmt, final IRNode thrownExpr) {
    // Do nothing
  }
  
  
  
  @Override
  public final Void visitSynchronizedStatement(final IRNode s) {
    /*
     * ¤14.19
     * 
     * A synchronized statement is executed by first evaluating the Expression.
     * Then:
     * 
     * f the value of the Expression is null, a NullPointerException is thrown.
     */
    doAcceptForChildren(s);
    checkSynchronizedStatement(s, SynchronizedStatement.getLock(s));
    return null;
  }
  
  protected void checkSynchronizedStatement(
      final IRNode syncStmt, final IRNode lockExpr) {
    // do nothing
  }

  
  
  // ======================================================================
  // == Expressions
  // ======================================================================
}
