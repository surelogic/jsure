/* $Header$ */
package edu.cmu.cs.fluid.java.operator;

import edu.cmu.cs.fluid.ir.IRNode;

public interface AssignmentInterface extends StatementExpressionInterface 
{
  public IRNode getSource(IRNode node);
  public IRNode getTarget(IRNode node);
}
