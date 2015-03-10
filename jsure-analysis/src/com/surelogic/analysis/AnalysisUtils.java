package com.surelogic.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Helper methods for analysis.  Perhaps I should find a better place for these.
 */
public final class AnalysisUtils {
  private AnalysisUtils() {
    // TODO Auto-generated constructor stub
  }


  
  /**
   * Get the receiver node appropriate for use at the given expression.
   * Normally this is the receiver node from the flow unit being analyzed,
   * unless the given node is inside a FieldDeclaration or ClassInitializer
   * that is itself inside an AnonClassExpression or EnumConstantDeclaration.
   * In that case, we use the receiver node from the InitMethod for the 
   * class expression.
   */
  public static IRNode getReceiverNodeAtExpression(
      final IRNode use, final IRNode flowUnit) {
    /* Need to determine if the use is inside a field init or init block
     * of an anonymous class expression.
     */
    IRNode getReceiverFrom = null;
    for (final IRNode current : VisitUtil.rootWalk(use)) {
      final Operator op = JJNode.tree.getOperator(current);
      if (ClassBody.prototype.includes(op)) {
        // done: skipped past anything potentially interesting
        getReceiverFrom = flowUnit;
        break;
      } else if (FieldDeclaration.prototype.includes(op) ||
          ClassInitializer.prototype.includes(op)) {
        /* Have to check against FieldDeclaration to avoid capturing local
         * variable initializers.  This cannot be used in a static context,
         * so don't even check for it
         */
        final IRNode enclosingType = VisitUtil.getEnclosingType(current);
        final Operator enclosingOp = JJNode.tree.getOperator(enclosingType);
        if (AnonClassExpression.prototype.includes(enclosingOp) ||
            EnumConstantClassDeclaration.prototype.includes(enclosingOp)) {
          getReceiverFrom = JavaPromise.getInitMethod(enclosingType);
          break;
        }
      }
    }
    return JavaPromise.getReceiverNode(getReceiverFrom);
  }
}
