package com.surelogic.analysis;

import com.surelogic.aast.java.ExpressionNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * Contains methods used to bind ThisExpressions to ReceiverDeclarations and
 * QualifiedThisExpressions to QualifiedReceiverDeclarations. This is not
 * performed by the normal IBinder implementations. This process is often
 * complicated by the fact that external knowledge about which constructor
 * should be considered to contain the expression is required. Without this
 * knowledge, when we try to bind a ThisExpression that appears in an instance
 * initializer, we use the canonical receiver that is part of the instance
 * initializer; but we are analyzing the initializer on behalf of a particular
 * constructor, so it should use the receivers associated with that constructor.
 * 
 * <p>Previously this process was called "fixing" the ThisExpression, but 
 * "binding" now seems more appropriate.
 */
public interface ThisExpressionBinder extends IBinder {
  /**
   * If the given expression is a ThisExpession or a QualifiedThisExpression,
   * convert it to a ReceiverDeclaration or a QuanlifiedReceiverDeclaration,
   * respectively.
   * 
   * @return The appropriate ReceiverDeclaration or
   *         QuanlifiedReceiverDeclaration node, or {@code expr} if the node is
   *         not a ThisExpression or QualifiedThisExpression. (In particular, if
   *         the node is already a ReceiverDeclaration or
   *         QualifiedReceiverDeclaration, it is passed through.)
   */
  public IRNode bindThisExpression(IRNode expr);

  /**
   * If the given expression is a ThisExpessionNode or a QualifiedThisExpressionNode,
   * convert it to a ReceiverDeclaration node or a QuanlifiedReceiverDeclaration node,
   * respectively.
   * 
   * @return The appropriate ReceiverDeclaration or
   *         QuanlifiedReceiverDeclaration node, or {@code null} if the node is
   *         not a ThisExpression or QualifiedThisExpression.
   */
  public IRNode bindThisExpression(ExpressionNode expr);
}
