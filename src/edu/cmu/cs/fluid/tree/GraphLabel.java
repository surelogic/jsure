/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/GraphLabel.java,v 1.4 2003/07/02 20:19:10 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRNode;

/** Classes implementing this interface label their nodes
 * with Operators which determine allowable parents and children.
 * These labels are immutable.
 * Classes implementing this interface usually also implement
 * <tt>DigraphInterface</tt>.
 * @see Operator
 * @see DigraphInterface
 * @see SyntaxTree
 */
public interface GraphLabel {
  /** Return operator of a tree node.
   * @precondition nonNull(node)
   */
  public Operator getOperator(IRNode node);
}
  
