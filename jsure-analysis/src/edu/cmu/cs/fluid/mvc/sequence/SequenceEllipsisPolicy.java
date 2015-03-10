package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.ir.IRNode;


/**
 * Interface for {@link ConfigurableSequenceView} ellipsis policy.  An
 * ellipsis policy determines where ellipses are placed in 
 * a view's exported model when nodes of the viewed model
 * are skipped (not exported).
 *
 * <p>When a <code>ConfigurableSequenceView</code> starts to build a new
 * model for export it first calls {@link #resetPolicy} on the
 * current ellipsis policy to tell the policy to delete any state
 * left over from the previous export.  In the course of building
 * the model, the ForestView will call the current policy's
 * {@link #nodeSkipped} method whenever a node in the view model is
 * not added to the model.  Once the <code>ConfigurableSequenceView</code>
 * is done adding nodes to model, it calls {@link #applyPolicy} which
 * causes the ellipsis policy to add ellipsis (if the policy decides
 * they are needed) to the model.
 *
 * <P>An ellipsis policy adds ellipses to a
 * <code>ConfigurableSequenceView</code>'s exported model by calling 
 * the methods {@link ConfigurableSequenceView#insertEllipsisAfter}
 * and {@link ConfigurableSequenceView#insertEllipsisBefore}.
 * Ellipses should only be added by {@link #applyPolicy}.  Because a 
 * policy must call back to the view, most policies will have
 * to be associated with a given view when they are constructed.
 *
 * @author Aaron Greenhouse
 */
public interface SequenceEllipsisPolicy
{
  /**
   * Removes any state in the policy that was based on the
   * previous sub-model.  The <code>ConfigurableSequenceView</code> is 
   * constructing a new model for export, and all calls to
   * {@link #nodeSkipped} between this call and the next call
   * to {@link #applyPolicy} relate to the new model.
   */
  public void resetPolicy();

  /**
   * Called by <code>ConfigurableSequenceView</code> to inform the policy
   * that a node present in the view model is not being added to
   * the exported model.  The ellipsis policy should use this
   * information to decide if ellipses need to be added to the
   * exported model.  <EM>This method must not add any ellipses to
   * the sub-model; only {@link #applyPolicy} is allowed to add
   * ellipses.</EM>
   * @param node The node that was not added to the exported model.
   * @param loc The sequence location <code>node</code> would have had
   * were it added to <code>parent</code>.
   */
  public void nodeSkipped( IRNode node, int loc );

  /**
   * Adds ellipses to the exported model.
   */
  public void applyPolicy();
}


