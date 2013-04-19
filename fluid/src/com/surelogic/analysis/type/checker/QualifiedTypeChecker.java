package com.surelogic.analysis.type.checker;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.ArrayRefExpression;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.BoxExpression;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.ReturnStatement;
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
    super();
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

  @Override
  public final Void visitReturnStatement(final IRNode s) {
    /*
     * ¤14.17
     * 
     * The Expression must denote a variable or value of some type T, or a
     * compile-time error occurs.
     * 
     * The type T must be assignable (¤5.2) to the declared result type of the
     * method, or a compile-time error occurs.
     */
    doAcceptForChildren(s);
    checkReturnStatement(s, ReturnStatement.getValue(s));
    return null;
  }
  
  protected void checkReturnStatement(
      final IRNode returnStmt, final IRNode valueExpr) {
    // Do nothing
  }
  
  
  
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
     * If the value of the Expression is null, a NullPointerException is thrown.
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
  
  // TODO: Class Instance Creation Expressions
  
  // TODO: Array Creation Expressions
  
  @Override
  public final Void visitFieldRef(final IRNode e) {
    /*
     * ¤15.11.1
     * 
     * If the field is not static: É If the value of the Primary is null, then a
     * NullPointerException is thrown.
     */
    doAcceptForChildren(e);
    checkFieldRef(e, FieldRef.getObject(e));
    return null;
  }
  
  protected void checkFieldRef(
      final IRNode fieldRefExpr, final IRNode objectExpr) {
    // do nothing
  }

  // TODO: Method invocation expressions
  
  @Override
  public final Void visitArrayRefExpression(final IRNode e) {
    /*
     * ¤15.13
     * 
     * Otherwise, if the value of the array reference expression is null, then a
     * NullPointerException is thrown.
     * 
     * Otherwise, the value of the array reference expression indeed refers to
     * an array. If the value of the index expression is less than zero, or
     * greater than or equal to the array's length, then an
     * ArrayIndexOutOfBoundsException is thrown.
     */
    doAcceptForChildren(e);
    checkArrayRefExpression(
        e, ArrayRefExpression.getArray(e), ArrayRefExpression.getIndex(e));
    return null;
  }
  
  protected void checkArrayRefExpression(final IRNode arrayRefExpr,
      final IRNode arrayExpr, final IRNode indexExpr) {
    // do nothing
  }
  
  @Override
  public final Void visitDivExpression(final IRNode e) {
    /*
     * ¤15.17.2
     * 
     * If the value of the divisor in an integer division is 0, then an
     * ArithmeticException is thrown.
     */
    doAcceptForChildren(e);
    checkDivExpression(e);
    return null;
  }
  
  protected void checkDivExpression(final IRNode divExpr) {
    // do nothing
  }
  
  @Override
  public final Void visitRemExpression(final IRNode e) {
    /*
     * ¤15.17.3
     * 
     * If the value of the divisor for an integer remainder operator is 0, then
     * an ArithmeticException is thrown.
     */
    doAcceptForChildren(e);
    checkRemExpression(e);
    return null;
  }
  
  protected void checkRemExpression(final IRNode remExpr) {
    // do nothing
  }
  
  @Override
  public final Void visitAssignExpression(final IRNode e) {
    /*
     * ¤15.26.1
     * 
     * A compile-time error occurs if the type of the right-hand operand cannot
     * be converted to the type of the variable by assignment conversion (¤5.2).
     * 
     * N.B. Most of this is actually handled by visitFieldRef() and
     * visitArrayRefExpression().
     * 
     * If the left-hand operand is an array access expression (¤15.13) then an
     * ArrayStoreException is raised if the run-time type of the object being
     * assigned to the array is a proper supertype of the run-time type of the
     * array element.
     */
    doAcceptForChildren(e);
    checkAssignExpression(
        e, AssignExpression.getOp1(e), AssignExpression.getOp2(e));
    return null;
  }
  
  protected void checkAssignExpression(
      final IRNode assignExpr, final IRNode lhs, final IRNode rhs) {
    // do nothing
  }
}
