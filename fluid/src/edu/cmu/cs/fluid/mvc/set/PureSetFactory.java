package edu.cmu.cs.fluid.mvc.set;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.SimpleComponentSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.ExplicitSlotFactory;

/**
 * Factory for creating instances of PureSet.  Models returned by the factory
 * implement only the minimum requirements of {@link SetModel}.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 *
 * @author Aaron Greenhouse
 */
public final class PureSetFactory
implements PureSet.Factory
{
  /**
   * The singleton reference.
   */
  public static final PureSet.Factory prototype = new PureSetFactory();
  
  
  
  /**
   * Constructor for subclassing only;
   * use the singleton reference {@link #prototype}.
   */
  private PureSetFactory()
  {
  }
  

  
  /**
   * Create a new PureSet model instance.
   * @param name The name of the model.
   * @param sf The slot factory to use to create the model's
   *           structural attributes.
   */
  @Override
  public PureSet create( final String name, final ExplicitSlotFactory sf )
  throws SlotAlreadyRegisteredException
  {
    return new PureSetImpl(
                 name, new ModelCore.StandardFactory( sf ),
                 new SetModelCore.StandardFactory(
                       sf, new SimpleComponentSlotFactory( sf ) ), sf );
  }
}
