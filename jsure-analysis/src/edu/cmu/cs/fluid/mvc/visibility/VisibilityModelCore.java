// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/VisibilityModelCore.java,v 1.15 2008/05/15 16:24:12 aarong Exp $

package edu.cmu.cs.fluid.mvc.visibility;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.ir.ConstantExplicitSlotFactory;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * Generic implementation of the methods declared in 
 * {@link VisibilityModel}.
 *
 * <p>Adds the model-level attribute {@link VisibilityModel#VISIBILITY_OF}.
 *
 * <p>Adds the node-level attribute {@link VisibilityModel#IS_VISIBLE}.
 *
 * @author Aaron Greenhouse
 */
public final class VisibilityModelCore
extends AbstractCore
{
  /*
   * The creation of the {@link VisibilityModel#IS_VISIBLE} attribute is 
   * deferred to the model, who must register it using the 
   * setIsVisibleAttribute method.
   */

  //===========================================================
  //== Fields
  //===========================================================
    
  /** Storage for the {@link VisibilityModel#IS_VISIBLE} Attribute. */
  private SlotInfo<Boolean> isVisible;

  /** Storage for the {@link VisibilityModel#VISIBILITY_OF} Attribute. */
  private final ComponentSlot<Model> visibilityOf;

  

  //===========================================================
  //== Constructors
  //===========================================================

  // Model must call {@link #setIsVisibleAttribute}
  protected VisibilityModelCore(
    final String name, final Model partOf, final Object lock, final Model visOf,
    final AttributeManager manager )
  throws SlotAlreadyRegisteredException
  {
    super( partOf, lock, manager );

    // Init model attributes
    visibilityOf = new SimpleComponentSlot<Model>( ModelType.prototype, 
                         ConstantExplicitSlotFactory.prototype, visOf );
    attrManager.addCompAttribute(
      VisibilityModel.VISIBILITY_OF, Model.STRUCTURAL, visibilityOf );
  }

  
  
  //===========================================================
  //== Methods
  //===========================================================

  /**
   * Set the core's reference to the attribute value storage for
   * the {@link VisibilityModel#IS_VISIBLE} attribute.  This will cause 
   * the attribute to be registered with the attribute manager.
   * @exception NullPointerExcetion Thrown if the provided value is null.
   * @exception IllegalStateException Thrown if the method has already been called.
   */
  public final void setIsVisibleAttribute( final SlotInfo<Boolean> si )
  {
    if( si == null ) {
      throw new NullPointerException( "Provided storage is null." );
    }
    if( isVisible != null ) {
      throw new IllegalStateException( "Method cannot be called more than once." );
    }
    
    isVisible = si;
  }

  /**
   * Get the value of the {@link VisibilityModel#IS_VISIBLE} attribute.
   */
  public boolean isVisible( final IRNode node )
  {
    if (node.valueExists( isVisible )) {
      Boolean val = node.getSlotValue( isVisible );
      if (val != null) {
	return val.booleanValue();
      }
    }
    return true;
  }

  /**
   * Set the value of the {@link VisibilityModel#IS_VISIBLE} attribute.
   * This method should only be used internally by a model implemenation,
   * and not be based through as part of the interface of the model.
   */
  public void setVisible( final IRNode node, final boolean val )
  {
    node.setSlotValue( isVisible, val ? Boolean.TRUE : Boolean.FALSE );
  }

  /**
   * Set the value of the {@link VisibilityModel#IS_VISIBLE} attribute.
   * This method should only be used internally by a model implemenation,
   * and not be based through as part of the interface of the model.
   */
  public void setVisible( final IRNode node, final Boolean val )
  {
    node.setSlotValue( isVisible, val );
  }

  

  //===========================================================
  //== ModelCore Factory Interfaces/Classes
  //===========================================================

  public static interface Factory 
  {
    public VisibilityModelCore create(
      String name, Model model, Object structLock,  Model visOf,
      AttributeManager manager )
    throws SlotAlreadyRegisteredException;
  }
  
  private final static class StandardFactory
  implements Factory
  {
    public StandardFactory() {}

    @Override
    public VisibilityModelCore create(
      final String name, final Model model, final Object structLock,
      final Model visOf, final AttributeManager manager )
    throws SlotAlreadyRegisteredException
    {
      return new VisibilityModelCore( name, model, structLock, visOf, manager );
    }
  }
  
  public static final Factory standardFactory = new StandardFactory();
}

