/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/syntax/DelegatingPureSyntaxForestFactory.java,v 1.10 2006/03/30 19:47:20 chance Exp $
 *
 * StandardPureForestFactory.java
 * Created on April 2, 2002, 1:28 PM
 */

package edu.cmu.cs.fluid.mvc.tree.syntax;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.tree.ForestForestModelCore;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;
import edu.cmu.cs.fluid.tree.*;

/**
 * Factory for creating {@link PureSyntaxForest} models that use a (pre-existing)
 * {@link edu.cmu.cs.fluid.tree.SyntaxTree} object as their syntax tree delegate.
 *
 * @author Aaron Greenhouse
 */
public final class DelegatingPureSyntaxForestFactory 
implements PureSyntaxForest.Factory
{
  /** Whether to create mutable models or not. */
  private final boolean isMutable;
  
  /** The tree to use as the underlying model. */
  private final SyntaxTreeInterface tree;
  
  /** The sequence of root nodes. */
  private final IRSequence<IRNode> rootsSeq;
  
  
  
  /**
   * Create a new factory object.
   * @param tr The syntax tree to use as the underlying model.
   * @param roots The sequence of roots in the forest.
   * @param mutable Whether to create mutable models or not.
   */
  public DelegatingPureSyntaxForestFactory(
    final SyntaxTreeInterface tr, final IRSequence<IRNode> roots, final boolean mutable )
  {
    tree = tr;
    rootsSeq = roots;
    isMutable = mutable;
  }
  
  
  
  @Override
  public PureSyntaxForest create( final String name, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {
    return new PureSyntaxForestImpl(
                 name, new ModelCore.StandardFactory( sf ),
                 new ForestForestModelCore.DelegatingFactory(
                       tree, rootsSeq, sf, isMutable ),
                 new SyntaxForestModelCore.StandardFactory( tree ), sf );
  }
}
