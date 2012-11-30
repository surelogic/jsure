// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/SyntaxTreeInterface.java,v 1.3 2003/07/02 20:19:08 thallora Exp $ 
package edu.cmu.cs.fluid.tree;

import com.surelogic.ThreadSafe;

import edu.cmu.cs.fluid.ir.IRNode;

/** Abstract interface for syntax trees (see @{link SyntaxTree} for more
 * details 
 *
 * @author Edwin Chan
 */
@ThreadSafe
public interface SyntaxTreeInterface extends MutableTreeInterface, GraphLabel {
  public boolean opExists(IRNode node);

  /** Create a node for the particular operator.
   * The number of children required is determined from the operator.
   */
  public void initNode(IRNode n, Operator op);
 
  /** Create a node for a particular operator, with minimum number of children
   * NB: no slots are mutated with this code.
   */
  public void initNode(IRNode n, Operator op, int min);

  /** Create a node for a particular operator, with particular children.
   * NB: no slots are mutated with this code.
   */
  public void initNode(IRNode n, Operator op, IRNode[] children);
}

