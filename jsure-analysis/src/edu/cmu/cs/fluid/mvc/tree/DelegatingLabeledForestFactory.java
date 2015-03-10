/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/DelegatingLabeledForestFactory.java,v 1.10 2006/03/30 19:47:20 chance Exp $
 *
 * StandardPureForestFactory.java
 * Created on April 2, 2002, 1:28 PM
 */

package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.Tree;

/**
 * Factory for creating {@link LabeledForest} models that use a (pre-existing)
 * {@link edu.cmu.cs.fluid.tree.Tree} object as their tree delegate.
 *
 * @author Aaron Greenhouse
 */
public final class DelegatingLabeledForestFactory
implements LabeledForest.Factory
{
  /** Whether to create mutable models or not. */
  private final boolean isMutable;
  
  /** The tree to use as the underlying model. */
  private final Tree tree;
  
  /** The sequence of roots in the forest. */
  //private final IRSequence rootsSeq;
  
  
  
  /**
   * Create a new factory object.
   * @param tr The tree to use as the underlying model.
   * @param roots The sequence of roots in the forest.
   * @param mutable Whether to create mutable models or not.
   */
  public DelegatingLabeledForestFactory(
    final Tree tr, final IRSequence roots, final boolean mutable )
  {
    tree = tr;
    //rootsSeq = roots;
    isMutable = mutable;
  }
  
  
  
  @Override
  public LabeledForest create( final String name, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {
    IRSequence<IRNode> seq = sf.newSequence(~0);
    return new LabeledForestImpl(
                 name, sf, new ModelCore.StandardFactory( sf ),
                 new ForestForestModelCore.DelegatingFactory(
                       tree, seq, sf, isMutable ) );
  }
}
