package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.ir.*;

import edu.cmu.cs.fluid.mvc.*;

/**
 * This class provides the (new) core functionality of a
 * {@link VersionSpaceToVersionTrackerStatefulView}.
 * 
 * @author Zia Syed
 * @author Aaron Greenhouse
 */

public final class VersionSpaceToVersionTrackerStatefulViewCore
extends AbstractCore
{
  /**
   * Storage for the
   * {@link VersionSpaceToVersionTrackerStatefulView#IS_FOLLOWING} Attribute.
   */
  private final ComponentSlot<Boolean> isFollowingAttr;
  
  
  
  //===========================================================
  //== Constructors
  //===========================================================	
  
  protected VersionSpaceToVersionTrackerStatefulViewCore(
    final Model model, final Object lock, final AttributeManager manager,
    final AttributeChangedCallback cb )
  throws SlotAlreadyRegisteredException
  {
    super( model, lock, manager );
    
    // Init model attributes
    isFollowingAttr = new SimpleComponentSlot<Boolean>( IRBooleanType.prototype,
                                               SimpleExplicitSlotFactory.prototype,
                                               Boolean.TRUE );
    attrManager.addCompAttribute(
      VersionSpaceToVersionTrackerStatefulView.IS_FOLLOWING, 
      Model.STRUCTURAL, isFollowingAttr, cb );
  }

  
  
  //===========================================================
  //== Attribute convienence methods
  //===========================================================	

  /**
   * Convienence method to set the
   * {@link VersionSpaceToVersionTrackerStatefulView#IS_FOLLOWING} attribute.
   */
  public void setFollowing( final boolean mode )
  {
    isFollowingAttr.setValue( mode ? Boolean.TRUE : Boolean.FALSE );
  }
  
  /**
   * Convienence method to get the
   * {@link VersionSpaceToVersionTrackerStatefulView#IS_FOLLOWING} attribute.
   */
  public boolean isFollowing()
  {
    return isFollowingAttr.getValue().booleanValue();
  }
  
  
  
  //===========================================================
  //== VersionTrackerModelCore Factory Interfaces/Classes
  //===========================================================

  public static interface Factory 
  {
    public VersionSpaceToVersionTrackerStatefulViewCore create(
      Model model, Object structLock, AttributeManager manager,
      AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException;
  }
	  
  private static class StandardFactory
  implements Factory
  {
    @Override
    public VersionSpaceToVersionTrackerStatefulViewCore create(
      final Model model, final Object structLock,
      final AttributeManager manager, final AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException
    {
      return new VersionSpaceToVersionTrackerStatefulViewCore(
                   model, structLock, manager, cb );
    }
  }	
	  
  public static final Factory standardFactory = new StandardFactory();  
}







