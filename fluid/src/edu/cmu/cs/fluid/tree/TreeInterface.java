/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/TreeInterface.java,v 1.7 2007/07/10 22:16:32 aarong Exp $ */
package edu.cmu.cs.fluid.tree;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** Classes implementing this interface provide routines
 * to traverse trees of IRNodes.
 * Each node may have at most one parent.
 * @see Tree
 */
public interface TreeInterface extends SymmetricDigraphInterface {
  /** Return parent of a tree node.
   * @precondition nonNull(node)
   */
  public IRNode getParent(IRNode node);

  /** The location is a value used by an IRSequence
   * to locate an element.  For IRArray, it is an integer.
   * @precondition nonNull(node)
   */
  public IRLocation getLocation(IRNode node);

  /** Return the root of a subtree.
   */
  public IRNode getRoot(IRNode subtree);

  /** Return an enumeration of nodes in the subtree
   * starting with leaves and working toward the node given.
   * A postorder traversal.
   */
  public Iteratable<IRNode> bottomUp(IRNode subtree);

  /** Return an enumeration of nodes in the subtree
   * starting with this node and working toward the leaves.
   * A preorder traversal.
   */
  public Iteratable<IRNode> topDown(IRNode subtree);
}
