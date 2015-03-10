/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/LabeledSetImpl.java,v 1.8 2007/07/05 18:15:18 aarong Exp $
 *
 * LabeledSetImpl.java
 * Created on February 28, 2002, 10:39 AM
 */

package edu.cmu.cs.fluid.mvc.set;

import edu.cmu.cs.fluid.mvc.AbstractAttributeChangedCallback;
import edu.cmu.cs.fluid.mvc.AttributeValuesChangedEvent;
import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.*;

/**
 * An implementation of a {@link LabeledSet} model.  
 *
 * @author Aaron Greenhouse
 */
final class LabeledSetImpl
extends AbstractSetModel
implements LabeledSet
{
  //===========================================================
  //== Fields
  //===========================================================

  /** Slot for the labels */
  private final SlotInfo<String> labels;
  
  
  
  //===========================================================
  //== Constructors
  //===========================================================

  protected LabeledSetImpl(
    final String name, final ModelCore.Factory mf,
    final SetModelCore.Factory smf, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, smf, sf );
    labels = sf.newAttribute( name + "-" + LABEL, IRStringType.prototype );
    attrManager.addNodeAttribute(
      LABEL, Model.INFORMATIONAL, labels, new LabelChangedCallback() );
    finalizeInitialization();
  }
  
  
  
  //===========================================================
  //== Attribute Convience Methods
  //===========================================================

  // Inherit Java doc
  @Override
  public void setLabel( final IRNode node, final String label )
  {
    synchronized( structLock ) {
      node.setSlotValue( labels, label );
    }
    modelCore.fireModelEvent( new AttributeValuesChangedEvent( this, node, LABEL, label ) );
  }

  // Inherit Java doc
  @Override
  public String getLabel( final IRNode node )
  {
    synchronized( structLock ) {
      return node.getSlotValue( labels );
    }
  }

  /**
   * Override to return the node's label (if it exists).
   */
  @Override
  public String idNode( final IRNode node )
  {
    try {
      return getLabel( node );
    } catch( final SlotUndefinedException e ) {
      // Label may not have been defined 
      return node.toString();
    }
  }



  //===========================================================
  //== Attribute Callback for Label Attribute
  //===========================================================

  private class LabelChangedCallback
  extends AbstractAttributeChangedCallback
  {
    @Override
    protected void attributeChangedImpl(
      final String attr, final IRNode node, final Object value )
    {
      if( attr == LABEL ) {
        modelCore.fireModelEvent(
          new AttributeValuesChangedEvent( LabeledSetImpl.this, node, attr, value ) );
      }
    }
  }
}
