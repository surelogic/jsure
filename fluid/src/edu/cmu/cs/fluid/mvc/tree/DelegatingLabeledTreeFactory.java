/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/DelegatingLabeledTreeFactory.java,v 1.7 2003/07/15 21:47:18 aarong Exp $
 *
 * StandardPureForestFactory.java
 * Created on April 2, 2002, 1:28 PM
 */

package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;
import edu.cmu.cs.fluid.tree.Tree;

/**
 * Factory for creating {@link LabeledTree} models that use a (pre-existing)
 * {@link edu.cmu.cs.fluid.tree.Tree} object as their tree delegate.  The creator
 * will have to explicitly add the root to the tree.
 *
 * @author Aaron Greenhouse
 */
public final class DelegatingLabeledTreeFactory
implements LabeledTree.Factory
{
  /** Whether to create mutable models or not. */
  private final boolean isMutable;
  
  /** The tree to use as the underlying model. */
  private final Tree tree;
  
  
  
  /**
   * Create a new factory object.
   * @param tr The tree to use as the underlying model.
   * @param mutable Whether to create mutable models or not.
   */
  public DelegatingLabeledTreeFactory( final Tree tr, final boolean mutable )
  {
    tree = tr;
    isMutable = mutable;
  }
  
  
  
  @Override
  public LabeledTree create( final String name, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {
    return new LabeledTreeImpl(
                 name, sf, new ModelCore.StandardFactory( sf ),
                 new TreeForestModelCore.DelegatingFactory(
                       tree, sf, isMutable ) );
  }
}
