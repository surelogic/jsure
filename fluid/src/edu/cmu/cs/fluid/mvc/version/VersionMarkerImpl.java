package edu.cmu.cs.fluid.mvc.version;

import java.util.*;
import com.surelogic.common.util.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.version.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * A minimal implementation of {@link VersionMarkerModel}.
 */
final class VersionMarkerImpl
extends AbstractModel
implements VersionMarkerModel
{
  private final VersionTrackerModelCore verCore;

  
  
  //===========================================================
  //== Constructor
  //===========================================================

  protected VersionMarkerImpl( 
    final String name, final ModelCore.Factory mf,
    final VersionTrackerModelCore.Factory vmf, final Version initVersion )
  throws SlotAlreadyRegisteredException
  {
    super( name, mf, LocalAttributeManagerFactory.prototype, 
           SimpleSlotFactory.prototype );
    verCore = vmf.create( name, this, structLock, attrManager, 
                          VersionTrackerModelCore.allVersionVerifier,
                          new VersionChangedCallback() );
    verCore.setVersion( initVersion );
    finalizeInitialization();
  }

  
  
  //===========================================================
  //== Attribute Callback for Label Attribute
  //===========================================================

  private final class VersionChangedCallback
  extends AbstractAttributeChangedCallback
  {
    @Override
    protected void attributeChangedImpl(
      final String attr, final IRNode node, final Object value )
    {
      if( attr == VERSION ) {
        modelCore.fireModelEvent(
          new AttributeValuesChangedEvent(
                VersionMarkerImpl.this, attr, value ) );
      }
    }
  }
  
	
  
  //===========================================================
  //== Attribute Convienence methods
  //===========================================================

  @Override
  public void setVersion( final Version ver )
  {
    synchronized( structLock ) {
      verCore.setVersion(ver);
    }
    modelCore.fireModelEvent(
      new AttributeValuesChangedEvent( this, VERSION, ver ) );
  }
	
  @Override
  public Version getVersion()
  {
    synchronized( structLock ) {
      return verCore.getVersion();
    }
  }

  
  
  //===========================================================
  //== Node methods
  //===========================================================

  /**
   * Model contains no nodes.
   */
  @Override
  public boolean isPresent( final IRNode node )
  {
    return false;
  }

  /**
   * Always returns an empty iterator because the model never contains
   * any nodes.
   */
  @Override
  public Iterator<IRNode> getNodes()
  {
    return new EmptyIterator<IRNode>();
  }

  /**
   * Does nothing; the model can never contain any nodes.
   */
  @Override
  public void addNode( final IRNode node, final AVPair[] attrs ) {}
  
  /**
   * Does nothing; the model can never contain any nodes.
   */
  @Override
  public void removeNode( final IRNode node ) {}
  
//  public void executeWithin(Runnable action) {
//    verCore.executeWithin(action);    
//  } 
}
