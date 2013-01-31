package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.version.*;

import edu.cmu.cs.fluid.mvc.*;

/**
 * This class provides the core (new) functionality for a
 * {@link VersionCursorModel}.
 * 
 * @author Zia Syed
 * @author Aaron Greenhouse
 */

public final class VersionTrackerModelCore
extends AbstractCore
{
  /** Prototype reference to the version verifier that accepts all versions. */
  public static final VersionVerifier allVersionVerifier =
    new AllVersionVerifier();
  
  /** Storage for the {@link VersionCursorModel#VERSION} Attribute. */
  private final ComponentSlot<Version> verAttr;

  /** Reference to the version verifier strategy */
  private final VersionVerifier versionVerifier;
//
//  /**
//   * Count of the number of times the present thread has called
//   * {@link #executeWithin}.  
//   */
//  /* Making this field volatile may break the rule that Cores aren't supposed
//   * to deal with their own protection, but not sure what else to do here.  It
//   * may be possible that this is unnecessary because the model implementation
//   * is still responsible for making sure that only one thread executes in the
//   * executeWithin at a time, so the memory model shouldn't hurt us, but 
//   * making it volatile insures the memory model won't hurt us.
//   */
//  private volatile int reentranceCount = 0;

  
  
  //===========================================================
  //== Constructors
  //===========================================================	
  
  protected VersionTrackerModelCore(
    final String name, final Model model, final Object lock,
    final AttributeManager manager, VersionVerifier vv,
    final AttributeChangedCallback cb )
  throws SlotAlreadyRegisteredException
  {
    super( model, lock, manager );
    versionVerifier = vv;
    
    // Init model-level attributes
    verAttr = new VersionAttribute(
                    versionVerifier,
                    new SimpleComponentSlot<Version>(
                          IRVersionType.prototype,
                          SimpleExplicitSlotFactory.prototype ) );
    attrManager.addCompAttribute(
      VersionTrackerModel.VERSION, Model.STRUCTURAL, verAttr, cb );
  }
 
  
  
  //===========================================================
  //== Interface for strategy that accepts or rejects versions
  //===========================================================	
 
  /**
   * Strategy used by {@link VersionTrackerModelCore} to accept or
   * reject changes to particular versions.
   */
  public static interface VersionVerifier
  {
    /**
     * Predicate determining whether a {@link VersionTrackerModel} may
     * be set to a particular version.
     */
    public boolean shouldChangeToVersion( Version v );
  }
  
  
  
  /**
   * {@link VersionTrackerModelCore.VersionVerifier} that accepts all versions.
   */
  private static final class AllVersionVerifier
  implements VersionVerifier
  {
    @Override
    public boolean shouldChangeToVersion( final Version v )
    {
      return true;
    }
  }
  
  
	
  //===========================================================
  //== Specialized storage for the Version attribute
  //===========================================================

  private static class VersionAttribute
  extends ComponentSlotWrapper<Version>
  {
    private VersionVerifier versionVerifier;
    
    public VersionAttribute( VersionVerifier vv, ComponentSlot<Version> cs )
    {
      super( cs );
      versionVerifier = vv;
    }
    
    @Override
    public Slot<Version> setValue( final Version v ) 
    throws IllegalArgumentException
    {
      if( versionVerifier.shouldChangeToVersion( v ) ) {
        return super.setValue( v );
      } else {
        throw new IllegalArgumentException(
                    "Version " + v + " is not acceptable." );
      }
    }
  }

  
    
  //===========================================================
  //== Attribute convienence methods
  //===========================================================	
  
  /**
   * Sets the version cursor to the specified version.
   */
  public void setVersion( final Version ver )
  {
    verAttr.setValue( ver );
  }
  
  /**
   * Gets the current version that the model represents.
   */
  public Version getVersion()
  {
    return verAttr.getValue();
  }

//  /**
//   * Fill this in...
//   * 
//   * <p>NB.  Doesn't touch model attribute values, etc, so we don't
//   * need to have the structLock held.
//   */
//  public Version executeWithin(Runnable action) {
//    LOG.info("Version before = "+Version.getVersion());
//    Version here = getVersion();
//    if( reentranceCount == 0 ) Version.saveVersion(here);
//  	reentranceCount += 1;
//    LOG.info("Version here = "+Version.getVersion());
//    try {
//      action.run();
//    } catch(Throwable t) {
//      LOG.error("Exception within executeWithin", t);
//    } finally {
//      Version end = Version.getVersion();
//      LOG.info("Version at end = "+end);
//      reentranceCount -= 1;
//      if( reentranceCount == 0 ) Version.restoreVersion();
//      LOG.info("Version after = "+Version.getVersion());
//      return end;
//    }
//  }
  
  //===========================================================
  //== VersionCursorModelCore Factory Interfaces/Classes
  //===========================================================

  public static interface Factory 
  {
    public VersionTrackerModelCore create(
      String name, Model model, Object structLock, AttributeManager manager,
      VersionVerifier vv, AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException;
  }
	  
  private final static class StandardFactory
  implements Factory
  {
    @Override
    public VersionTrackerModelCore create(
      final String name, final Model model, final Object structLock,
      final AttributeManager manager, final VersionVerifier vv,
      final AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException
    {
      return new VersionTrackerModelCore(
                   name, model, structLock, manager, vv, cb );
    }
  }	
	  
  public static final Factory standardFactory = new StandardFactory();  
}
