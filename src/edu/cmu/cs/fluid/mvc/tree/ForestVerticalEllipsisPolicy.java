package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.ir.IRNode;


/**
 * Interface for <code>ForestView</code> ellipsis policy.  An
 * ellipsis policy determines where ellipses are placed in 
 * a view's exported model when nodes of the viewed model
 * are skipped (not exported).
 *
 * <p>When a <code>ConfigurableForestView</code> starts to build a new
 * model for export it first calls {@link #resetPolicy} on the
 * current ellipsis policy to tell the policy to delete any state
 * left over from the previous export.  In the course of building
 * the model, the ForestView will call the current policy's
 * {@link #nodeSkipped} method whenever a node in the view model is
 * not added to the model.  Once the <code>ConfigurableForestView</code>
 * is done adding nodes to model, it calls {@link #applyPolicy} which
 * causes the ellipsis policy to add ellipsis (if the policy decides
 * they are needed) to the model.
 *
 * <P>An ellipsis policy adds ellipses to a
 * <code>ConfigurableForestView</code>'s exported model by calling 
 * the method {@link ConfigurableForestView#insertEllipsisAt(IRNode, InsertionPoint, Set)}.
 * Ellipses should only be added by {@link #applyPolicy}.  Because a 
 * policy must call back to the view, most policies will have
 * to be associated with a given view when they are constructed.
 *
 * @author Aaron Greenhouse
 */
public interface ForestVerticalEllipsisPolicy
{
  /**
   * Removes any state in the policy that was based on the previous sub-model.
   * The <code>ConfigurableForestView</code> is constructing a new model for
   * export, and all calls to {@link #nodeSkipped}between this call and the
   * next call to {@link #resetPolicy}relate to the new model.
   */
  public void resetPolicy();

  /**
   * Called by <code>ConfigurableForestView</code> to inform the policy that a
   * node present in the view model is not being added to the exported model.
   * The ellipsis policy should use this information to decide if ellipses need
   * to be added to the exported model. Unlike
   * {@link ForestEllipsisPolicy#nodeSkipped(IRNode, IRNode, int)}, this method
   * is allowed to create new ellipsis nodes using
   * {@link ConfigurableForestView#createEllipsisNode()}. It must not set the
   * parent of the ellipsis node, however. That is still the job of
   * {@link #applyPolicy()}. This method returns the node to be used as the
   * parent of the children of the skipped node: this is either the incoming
   * parent, or an ellipsis.
   * 
   * @param node
   *          The node that was not added to the exported model.
   * @param parent
   *          The node that would have been <code>node</code>'s parent in the
   *          exported model. This node is guaranteed to be present in the
   *          exported model.
   * @param newPos
   *          The child position <code>node</code> would have had were it
   *          added to <code>parent</code>.
   * @param oldPos 
   *          The child position <code>node</code> had in the source model.
   * @return The node to used as the parent of the children of <code>node</code>.
   */
  public IRNode nodeSkipped( IRNode node, IRNode parent, int newPos, int oldPos );

  /**
   * Finalize the ellipses added to the exported model by
   * {@link #nodeSkipped(IRNode, IRNode, int, int)}.
   */
  public void applyPolicy();
}


