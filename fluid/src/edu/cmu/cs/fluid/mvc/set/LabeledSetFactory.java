/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/LabeledSetFactory.java,v 1.7 2004/09/10 17:33:53 boyland Exp $
 *
 * LabeledSetFactory.java
 * Created on February 28, 2002, 10:39 AM
 */

package edu.cmu.cs.fluid.mvc.set;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.SimpleComponentSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.ExplicitSlotFactory;

/**
 * Factory for creating instances of LabeledSet.  Models returned by the factory
 * implement only the minimum requirements of {@link LabeledSet}.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 *
 * @author Aaron Greenhouse
 */
public final class LabeledSetFactory
implements LabeledSet.Factory
{
  /**
   * The singleton reference.
   */
  public static final LabeledSet.Factory prototype = new LabeledSetFactory();
  
  
  
  /**
   * Use the singleton reference {@link #prototype}.
   */
  private LabeledSetFactory()
  {
  }
  

  /**
   * Create a new LabeledSet model instance.
   * @param name The name of the model.
   * @param sf The slot factory to use to create the model's
   *           structural and informational (e.g., {@link LabeledSet#LABEL})
   *           attributes.
   */
  @Override
  public LabeledSet create( final String name, final ExplicitSlotFactory sf )
  throws SlotAlreadyRegisteredException
  {
    return new LabeledSetImpl(
                 name, new ModelCore.StandardFactory( sf ),
                 new SetModelCore.StandardFactory(
                       sf, new SimpleComponentSlotFactory( sf ) ), sf );
  }
}