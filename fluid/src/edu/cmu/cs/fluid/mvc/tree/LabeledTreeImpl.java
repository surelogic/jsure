package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.AbstractAttributeChangedCallback;
import edu.cmu.cs.fluid.mvc.AttributeValuesChangedEvent;
import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.*;

/**
 * Minimal implementation of {@link LabeledTree}.
 *
 * @author Aaron Greenhouse
 */
final class LabeledTreeImpl
extends AbstractMutableForestModel
implements LabeledTree
{
  private final Bundle b = new Bundle();

  /** Slot for the labels */
  private final SlotInfo<String> labels;



  //===========================================================
  //== Constructor
  //===========================================================

  protected LabeledTreeImpl(
    final String name, final SlotFactory sf, final ModelCore.Factory mf,
    final ForestModelCore.Factory fmf )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, fmf, sf );
    labels = sf.newAttribute( name + "-" + LABEL, IRStringType.prototype );
    attrManager.addNodeAttribute(
      LABEL, Model.INFORMATIONAL, labels, new LabelChangedCallback() );
    b.saveAttribute(labels);
    finalizeInitialization();
  }

  
  
  public void setLabel( final IRNode node, final String label )
  {
    synchronized( structLock ) {
      node.setSlotValue( labels, label );
    }
    modelCore.fireModelEvent(
      new AttributeValuesChangedEvent( this, node, LABEL, label ) );
  }

  public String getLabel( final IRNode node )
  {
    synchronized( structLock ) {
      return node.getSlotValue( labels );
    }
  }

  
  
  /**
   * Override to return the node's label.
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
          new AttributeValuesChangedEvent(
                LabeledTreeImpl.this, node, attr, value ) );
      }
    }
  }
}
