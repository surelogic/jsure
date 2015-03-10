/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/syntax/StandardPureSyntaxForestFactory.java,v 1.10 2007/01/18 16:39:40 chance Exp $
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
import edu.cmu.cs.fluid.tree.SyntaxTree;

/**
 * Factory for creating {@link PureSyntaxForest} models that have their own,
 * unique underlying {@link edu.cmu.cs.fluid.tree.SyntaxTree} delegate.
 * 
 * <p>The class uses singleton patterns, and thus has a private
 * constructor. 
 *
 * @author Aaron Greenhouse
 */
public final class StandardPureSyntaxForestFactory
implements PureSyntaxForest.Factory
{
  /** Factory prototype for creating mutable models. */
  public static final PureSyntaxForest.Factory mutablePrototype =
    new StandardPureSyntaxForestFactory( true );
  
  /** Factory prototype for creating immutable models. */
  public static final PureSyntaxForest.Factory immutablePrototype =
    new StandardPureSyntaxForestFactory( false );
  
  
  
  /** Whether to create mutable models or not. */
  private final boolean isMutable;
  
  
  
  /** 
   * Use {@link #mutablePrototype} or {@link #immutablePrototype}.
   */
  private StandardPureSyntaxForestFactory( final boolean mutable )
  {
    isMutable = mutable;
  }
  
  @Override
  public PureSyntaxForest create( final String name, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {
    final SyntaxTree tree = new SyntaxTree( name + "-tree_delegate", sf );
    final IRSequence<IRNode> roots = sf.newSequence( -1 );
    
    return new PureSyntaxForestImpl(
                 name, new ModelCore.StandardFactory( sf ),
                 new ForestForestModelCore.DelegatingFactory(
                       tree, roots, sf, isMutable ), 
                 new SyntaxForestModelCore.StandardFactory( tree ), sf );
  }
}
