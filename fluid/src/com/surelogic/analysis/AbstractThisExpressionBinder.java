/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/AbstractThisExpressionBinder.java,v 1.2 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis;

import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.QualifiedThisExpressionNode;
import com.surelogic.aast.java.SuperExpressionNode;
import com.surelogic.aast.java.ThisExpressionNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.BinderWrapper;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.util.PromiseUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public abstract class AbstractThisExpressionBinder extends BinderWrapper implements ThisExpressionBinder {  
  protected AbstractThisExpressionBinder(final IBinder b) {
	super(b);
  }

  /**
   * @param node a ThisExpression, SuperExpression, MethodDeclaration, or ConstructorDeclaration node.
   */
  protected final IRNode defaultBindReceiver(final IRNode node) {
    return PromiseUtil.getReceiverNode(node);
  }
  
  /**
   * @param node a ThisExpression, SuperExpression, MethodDeclaration, or ConstructorDeclaration node.
   */
  protected final IRNode defaultBindQualifiedReceiver(final IRNode outerType, final IRNode node) {
    return PromiseUtil.getQualifiedReceiverNode(node, outerType);
  }

  /**
   * @param node a ThisExpression, SuperExpression, MethodDeclaration, or ConstructorDeclaration node.
   */
  protected abstract IRNode bindReceiver(IRNode node);
  
  /**
   * @param node a ThisExpression, SuperExpression, MethodDeclaration, or ConstructorDeclaration node.
   */
  protected abstract IRNode bindQualifiedReceiver(IRNode outerType, IRNode node);
  
  @Override
  public final IRNode bindThisExpression(final IRNode expr) {
	  IRNode result = bindThisExpression_private(expr);
	  return result;
  }
  
  private final IRNode bindThisExpression_private(final IRNode expr) {
    if (expr == null) {
      return null;
    } else {
      final Operator op = JJNode.tree.getOperator(expr);
      if (ThisExpression.prototype.includes(op)
          || SuperExpression.prototype.includes(op)) {
        /* We assume sanity here: that is, that "this" or "super" is not
         * appearing in a static method.
         */
        return bindReceiver(expr);
      } else if (QualifiedThisExpression.prototype.includes(op)) {
        final IRNode outerType =
          binder.getBinding(QualifiedThisExpression.getType(expr));
        /* We assume sanity here: that is, that a qualified receiver is not
         * appearing in a static method.
         */
        return bindQualifiedReceiver(outerType, expr);
      } else {
        return expr;
      }
    }
  }
  
  @Override
  public final IRNode bindThisExpression(final ExpressionNode expr) {
    if (expr == null) {
      return null;
    } else {
      if (expr instanceof ThisExpressionNode
          || expr instanceof SuperExpressionNode) {
        return bindReceiver(expr.getPromisedFor());
      } else if (expr instanceof QualifiedThisExpressionNode) {
        QualifiedThisExpressionNode qthis = (QualifiedThisExpressionNode) expr;
        final IRNode outerType = qthis.getType().resolveType().getNode();
        return bindQualifiedReceiver(outerType, expr.getPromisedFor());
      } else {
        return null;
      }
    }
  }
}
