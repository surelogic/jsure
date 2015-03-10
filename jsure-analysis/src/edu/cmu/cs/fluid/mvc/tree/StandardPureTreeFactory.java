/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/StandardPureTreeFactory.java,v 1.7 2007/01/18 16:39:40 chance Exp $
 *
 * StandardPureTreeFactory.java
 * Created on April 2, 2002, 1:28 PM
 */

package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Factory for creating {@link PureTree} models that have their own, unique
 * underlying {@link edu.cmu.cs.fluid.tree.Tree} delegate.
 * 
 * <p>The class uses singleton patterns, and thus has a private
 * constructor.
 *
 * @author Aaron Greenhouse
 */
public final class StandardPureTreeFactory
implements PureTree.Factory
{
  /** Factory prototype for creating mutable models. */
  public static final PureTree.Factory mutablePrototype =
    new StandardPureTreeFactory( true );
  
  /** Factory prototype for creating immutable models. */
  public static final PureTree.Factory immutablePrototype =
    new StandardPureTreeFactory( false );
  
  
  
  /** Whether to create mutable models or not. */
  private final boolean isMutable;
  
  
  
  /** 
   * Use {@link #mutablePrototype} or {@link #immutablePrototype}.
   */
  private StandardPureTreeFactory(final boolean mutable)
  {
    isMutable = mutable;
  }
  
  @Override
  public PureTree create( final String name, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {
    return new PureTreeImpl(
                 name, new ModelCore.StandardFactory( sf ),
                 new TreeForestModelCore.StandardFactory( sf, isMutable ), sf );
  }
}
