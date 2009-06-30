// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/AbstractModelToAttributeStatefulView.java,v 1.11 2006/03/29 18:30:56 chance Exp $
package edu.cmu.cs.fluid.mvc.attr;

import java.util.Iterator;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.set.AbstractModelToSetStatefulView;
import edu.cmu.cs.fluid.mvc.set.SetModelCore;
import edu.cmu.cs.fluid.ir.*;

/**
 * <em>Should probably do something about listening for
 * new attributes</em>.
 * @author Edwin Chan
 */
public abstract class AbstractModelToAttributeStatefulView
extends AbstractModelToSetStatefulView
{
  /** The AttributeModelCore delegate */
  protected final AttributeModelCore attrModCore;

  /**
   * The source model; the model whose attributes this model
   * is modeling.
   */
  protected final Model srcModel;



  //===========================================================
  //== Constructor
  //===========================================================

  public AbstractModelToAttributeStatefulView(
    final String name, final Model src, final ModelCore.Factory mf,
    final ViewCore.Factory vf, final SetModelCore.Factory smf, 
    final AttributeModelCore.Factory amf )
  throws SlotAlreadyRegisteredException
  {
    super( name, mf, vf, smf,
           LocalAttributeManagerFactory.prototype,
           NullAttributeInheritanceManagerFactory.prototype );
    attrModCore = amf.create( name, this, structLock, attrManager, 
                              new AttrAttrsChangedCallback() );
    srcModel = src;

    // Initialize Model-level attributes
    attrModCore.setAttributesOf( srcModel );
    final IRSequence<Model> srcModels = ConstantSlotFactory.prototype.newSequence(1);
    srcModels.setElementAt( src, 0 );
    viewCore.setSourceModels( srcModels );
  }

  

  //===========================================================
  //== Callback
  //===========================================================

  private class AttrAttrsChangedCallback
  extends AbstractAttributeChangedCallback
  {
    @Override
    protected void attributeChangedImpl(
      final String attr, final IRNode node, final Object val )
    {
      if( (attr == AttributeModel.ATTR_LABEL) ) {
	final ModelEvent e = 
	  new AttributeValuesChangedEvent(
                AbstractModelToAttributeStatefulView.this, node, attr, val );
        modelCore.fireModelEvent( e );
      }
    }
  }

  

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin AttributeModel portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== Attribute Convienence methods  
  //===========================================================

  // Inherit JavaDoc from AttributeModel
  public final String getName( final IRNode node )
  {
    synchronized( structLock ) {
      return attrModCore.getName( node );
    }
  }

  // Inherit JavaDoc from AttributeModel
  public final String getLabel( final IRNode node )
  {
    synchronized( structLock ) {
      return attrModCore.getLabel( node );
    }
  }

  // Inherit JavaDoc from AttributeModel
  public final void setLabel( final IRNode node, final String label )
  {
    synchronized( structLock ) {
      attrModCore.setLabel( node, label );
    }
    modelCore.fireModelEvent(
      new AttributeValuesChangedEvent( 
           this, node, AttributeModel.ATTR_LABEL, label ) );
  }

  // Inherit JavaDoc from AttributeModel
  public final IRType getType( final IRNode node )
  {
    synchronized( structLock ) {
      return attrModCore.getType( node );
    }
  }

  // Inherit JavaDoc from AttributeModel
  public final int getKind( final IRNode node )
  {
    synchronized( structLock ) {
      return attrModCore.getKind( node );
    }
  }

  // Inherit JavaDoc from AttributeModel
  public final boolean isMutable( final IRNode node )
  {
    synchronized( structLock ) {
      return attrModCore.isMutable( node );
    }
  }

  // Inherit JavaDoc from AttributeModel
  public final IRSequence getDomain( final IRNode node )
  {
    synchronized( structLock ) {
      return attrModCore.getDomain( node );
    }
  }

  // Inherit JavaDoc from AttributeModel
  public final boolean isNodeAttr( final IRNode node )
  {
    synchronized( structLock ) {
      return attrModCore.isNodeAttr( node );
    }
  }

  
  
  //===========================================================
  //== Methods to assist in the construction of the model's state
  //===========================================================

  /**
   * Remove all the nodes from the model.
   */
  protected final void clearModel()
  {
    Iterator<IRNode> it = setModCore.getNodes();
    while( it.hasNext() ) {
      IRNode n = it.next();
      setModCore.removeNode(n);
    }
  }



  //===========================================================
  //== Pickled State methods
  //===========================================================

  /**
   * Get pickled representation of the model's current state.
   */
  public final PickledAttributeModelState getPickledState()
  {
    synchronized( structLock ) {
      return attrModCore.getPickledState();
    }
  }

  /**
   * State the state of the attribute model from a
   * pickled representation of the state.
   * Any attributes present in the pickle that are not currently present
   * in the model are ignored.  Any attributes in the model that are
   * not present in the pickle are moved to the end of the sequence,
   * with their relative order retained.
   * @exception IllegalArgumentException Thrown if the 
   * pickle did not come from this model.
   */
  public final void setStateFromPickle( final PickledAttributeModelState pickle )
  {
    synchronized( structLock ) {
      attrModCore.setStateFromPickle( pickle );
    }
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End AttributeModel portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
}

