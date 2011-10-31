package edu.cmu.cs.fluid.java.util;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.AllocationExpression;
import edu.cmu.cs.fluid.java.operator.CastExpression;
import edu.cmu.cs.fluid.java.operator.NullLiteral;
import edu.cmu.cs.fluid.java.operator.OuterObjectSpecifier;
import edu.cmu.cs.fluid.java.operator.ParenExpression;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public final class OpUtil {
  private OpUtil() {
    super();
  }

  /**
   * Test if an expression is an AllocationExpression.  Unwraps ParenExpression
   * and CastExpressions.
   */
  public static boolean isAllocationExpression(final IRNode expr) {
    return openParensAndTestForOperator(AllocationExpression.prototype, expr);
  }

  /**
   * Test if an expression is a NullLiteral.  Unwraps ParenExpression
   * and CastExpressions.
   */
  public static boolean isNullExpression(final IRNode expr) {
    return openParensAndTestForOperator(NullLiteral.prototype, expr);
  }

  /**
   * Test if an expression is an AllocationExpression.  Unwraps ParenExpression
   * and CastExpressions.
   */
  public static boolean isOuterObjectSpecifier(final IRNode expr) {
    return openParensAndTestForOperator(OuterObjectSpecifier.prototype, expr);
  }

  public static boolean openParensAndTestForOperator(
      final Operator testFor, final IRNode expr) {
    final Operator op = JJNode.tree.getOperator(expr);
    if (testFor.includes(op)) {
      return true;
    } else if (ParenExpression.prototype.includes(op)) {
      return openParensAndTestForOperator(testFor, ParenExpression.getOp(expr));
    } else if (CastExpression.prototype.includes(op)) {
      return openParensAndTestForOperator(testFor, CastExpression.getExpr(expr));
    } else {
      return false;
    }
  }
}
