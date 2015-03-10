/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/PropagateUpTree.java,v 1.1 2007/05/25 02:12:42 boyland Exp $*/
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.ChangeRecord;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PropagateChanges;

/**
 * Propagate change information up the tree.
 * This class attaches itself to a change info structure,
 * and causes it to propagate changes up a tree.
 * It is not necessary to keep a reference to the instance of this class.
 * @author boyland
 */
public class PropagateUpTree extends PropagateChanges {

  private final Tree tree;
  
  /**
   * Cause the change info to be propagated up a tree.
   * @param ci
   */
  public static void attach(ChangeRecord ci, Tree tree) {
    new PropagateUpTree(ci,tree);
  }
  
  /**
   * Create a propagator that pushes changes bits up a tree.
   * @param ci
   */
  private PropagateUpTree(ChangeRecord ci, Tree t) {
    super(ci);
    tree = t;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.version.PropagateChangeInfo#noteChange(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  protected void noteChange(IRNode n) {
    propagateChange(tree.getParentOrNull(n));
  }

}
