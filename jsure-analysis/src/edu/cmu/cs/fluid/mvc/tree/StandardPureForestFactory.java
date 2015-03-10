/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/StandardPureForestFactory.java,v 1.7 2007/01/18 16:39:40 chance Exp $
 *
 * StandardPureForestFactory.java
 * Created on April 2, 2002, 1:28 PM
 */

package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Factory for creating {@link PureForest} models that have their own, unique
 * underlying {@link edu.cmu.cs.fluid.tree.Tree} delegate.
 * 
 * <p>The class uses singleton patterns, and thus has a private
 * constructor. 
 * @author Aaron Greenhouse
 */
public final class StandardPureForestFactory
implements PureForest.Factory
{
  /** Factory prototype for creating mutable models. */
  public static final PureForest.Factory mutablePrototype =
    new StandardPureForestFactory( true );
  
  /** Factory prototype for creating immutable models. */
  public static final PureForest.Factory immutablePrototype =
    new StandardPureForestFactory( false );
  
  
  
  /** Whether to create mutable models or not. */
  private final boolean isMutable;
  
  
  
  /** 
   * Use {@link #mutablePrototype} or {@link #immutablePrototype}.
   */
  private StandardPureForestFactory( final boolean mutable )
  {
    isMutable = mutable;
  }
  
  @Override
  public PureForest create( final String name, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {
    return new PureForestImpl(
                 name, new ModelCore.StandardFactory( sf ),
                 new ForestForestModelCore.StandardFactory( sf, isMutable ), sf );
  }
}
