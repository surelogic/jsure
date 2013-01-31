/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/StandardPureDigraphFactory.java,v 1.4 2007/01/18 16:39:40 chance Exp $
 *
 * StandardPureForestFactory.java
 * Created on April 2, 2002, 1:28 PM
 */

package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Factory for creating {@link PureDigraph} models that have their own, unique
 * underlying {@link edu.cmu.cs.fluid.tree.Digraph} delegate.
 * 
 * <p>The class uses singleton patterns, and thus has a private
 * constructor. 
 * @author Aaron Greenhouse
 */
public final class StandardPureDigraphFactory
implements PureDigraph.Factory
{
  /** Factory prototype for creating mutable models. */
  public static final PureDigraph.Factory mutablePrototype =
    new StandardPureDigraphFactory( true );
  
  /** Factory prototype for creating immutable models. */
  public static final PureDigraph.Factory immutablePrototype =
    new StandardPureDigraphFactory( false );
  
  
  
  /** Whether to create mutable models or not. */
  private final boolean isMutable;
  
  
  
  /** 
   * Use {@link #mutablePrototype} or {@link #immutablePrototype}.
   */
  private StandardPureDigraphFactory( final boolean mutable )
  {
    isMutable = mutable;
  }
  
  @Override
  public PureDigraph create( final String name, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {
    return new PureDigraphImpl(
                 name, new ModelCore.StandardFactory( sf ),
                 new DigraphModelCore.StandardFactory( sf, isMutable ), sf );
  }
}
