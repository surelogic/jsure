/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/NoEllipsisForestEllipsisPolicy.java,v 1.9 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * An ellipsis policy that never adds ellipses to the sub-model.
 * Because this policy does not need to call back the view, 
 * its instances do not need to be associated with a specific
 * view.  The implementation, therefore, enforces the singleton
 * pattern by making the constructor <code>private</code>,
 * and a single instance of the policy available through
 * the <code>public static final</code> {@link #prototype} field.
 *
 * @author Aaron Greenhouse
 */
public final class NoEllipsisForestEllipsisPolicy
implements ForestEllipsisPolicy
{
  /** Prototype for the policy. */
  public static final NoEllipsisForestEllipsisPolicy prototype = new NoEllipsisForestEllipsisPolicy();

  private NoEllipsisForestEllipsisPolicy()
  {
    super();
  }

  @Override
  public void resetPolicy()
  {
  }

  @Override
  public void nodeSkipped( final IRNode node, final IRNode parent, int pos )
  {
  }

  @Override
  public void applyPolicy()
  {
  }

  @Override
  public String toString() { return "No ellipses"; }
}
