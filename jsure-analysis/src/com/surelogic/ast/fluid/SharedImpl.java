/*$Header$*/
package com.surelogic.ast.fluid;

import java.util.*;

import com.surelogic.ast.AssignOperator;

import edu.cmu.cs.fluid.java.JavaOperator;
import edu.cmu.cs.fluid.java.operator.*;

public class SharedImpl {
  private static Map<JavaOperator,AssignOperator> assignMap = new HashMap<JavaOperator,AssignOperator>();
  static {
    assignMap.put(AndExpression.prototype, AssignOperator.AND);
    assignMap.put(OrExpression.prototype, AssignOperator.OR);
    assignMap.put(XorExpression.prototype, AssignOperator.XOR);
    assignMap.put(DivExpression.prototype, AssignOperator.DIV);
    assignMap.put(LeftShiftExpression.prototype, AssignOperator.LEFT_SHIFT);
    assignMap.put(SubExpression.prototype, AssignOperator.MINUS);
    assignMap.put(AddExpression.prototype, AssignOperator.PLUS);
    assignMap.put(RemExpression.prototype, AssignOperator.MOD);
    assignMap.put(RightShiftExpression.prototype, AssignOperator.RIGHT_SHIFT);
    assignMap.put(UnsignedRightShiftExpression.prototype, AssignOperator.UNSIGNED_RIGHT_SHIFT);
    assignMap.put(MulExpression.prototype, AssignOperator.MULT);
    assignMap.put(StringConcat.prototype, AssignOperator.CONCAT);
  }
  public static AssignOperator translateToAssignOp(JavaOperator op) {
    AssignOperator rv = assignMap.get(op);
    if (rv == null) {
      System.out.println("SEVERE: no mapping for "+op.name());
    }
    return rv;
  }
}
