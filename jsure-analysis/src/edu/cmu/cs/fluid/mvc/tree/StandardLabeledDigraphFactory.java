/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/StandardLabeledDigraphFactory.java,v 1.4 2007/01/18 16:39:40 chance Exp $
 *
 * StandardPureForestFactory.java
 * Created on April 2, 2002, 1:28 PM
 */

package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Factory for creating {@link LabeledDigraph} models that have their own, unique
 * underlying {@link edu.cmu.cs.fluid.tree.Digraph} delegate.
 * 
 * <p>The class uses singleton patterns, and thus has a private
 * constructor. 
 *
 * @author Aaron Greenhouse
 */
public final class StandardLabeledDigraphFactory
implements LabeledDigraph.Factory
{
  /** Factory prototype for creating mutable models. */
  public static final LabeledDigraph.Factory mutablePrototype =
    new StandardLabeledDigraphFactory( true );
  
  /** Factory prototype for creating immutable models. */
  public static final LabeledDigraph.Factory immutablePrototype =
    new StandardLabeledDigraphFactory( false );
  
  
  
  /** Whether to create mutable models or not. */
  private final boolean isMutable;
  
  
  
  /** 
   * Use {@link #mutablePrototype} or {@link #immutablePrototype}.
   */
  private StandardLabeledDigraphFactory( final boolean mutable )
  {
    isMutable = mutable;
  }
  
  @Override
  public LabeledDigraph create( final String name, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {
    return new LabeledDigraphImpl(
                 name, sf, new ModelCore.StandardFactory( sf ),
                 new DigraphModelCore.StandardFactory( sf, isMutable ) );
  }
}
