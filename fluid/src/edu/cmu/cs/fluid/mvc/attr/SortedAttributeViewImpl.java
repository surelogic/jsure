/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/SortedAttributeViewImpl.java,v 1.9 2007/07/10 22:16:39 aarong Exp $
 *
 * SortedAttributeViewImpl.java
 * Created on March 7, 2002, 4:08 PM
 */

package edu.cmu.cs.fluid.mvc.attr;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.sequence.AbstractSortedView;
import edu.cmu.cs.fluid.mvc.sequence.SequenceModelCore;
import edu.cmu.cs.fluid.ir.*;

/**
 * Minimum implementation of {@link SortedAttributeView}.
 *
 * @author Aaron Greenhouse
 */
final class SortedAttributeViewImpl
extends AbstractSortedView
implements SortedAttributeView
{
  //===========================================================
  //== Fields
  //===========================================================

  /** Storage for the names of attributes. */
  private final SlotInfo<String> attrNames;

  /** Storage for the labels of attributes. */
  private final SlotInfo<String> attrLabels;

  /** Storage for the types of attributes. */
  private final SlotInfo<IRType> attrTypes;

  /** Storage for the kinds of attributes. */
  private final SlotInfo<Integer> attrKinds;

  /** Storage for the mutable attribute */
  private final SlotInfo<Boolean> isMutable;
  
  /** Storage fro the domain attribute */
  private final SlotInfo<IRSequence> domain;

  /** Storage for the node attr attribute */
  private final SlotInfo<Boolean> isNodeAttr;

  /** Storage for the source model. */
  //private final ComponentSlot<Model> attrsOf;

  
  
  // Checks for legitimacy of sortAttr
  // Factory is responsible for making sure the attribute inheritnace policy
  // contains the attribute model attributes, and for controlling the 
  // mutability of the label and display attributes
  @SuppressWarnings("unchecked")
  public SortedAttributeViewImpl(
    final String name, final AttributeModel src, final ModelCore.Factory mf,
    final ViewCore.Factory vf, final SequenceModelCore.Factory smf, 
    final AttributeInheritancePolicy policy, final String attr,
    final boolean isAsc )
  throws SlotAlreadyRegisteredException
  {
    super( name, src, mf, vf, smf, policy, attr, isAsc );
    //attrsOf = attrManager.getCompAttribute( AttributeModel.ATTRIBUTES_OF );
    attrNames = attrManager.getNodeAttribute( AttributeModel.ATTR_NAME );
    attrKinds = attrManager.getNodeAttribute( AttributeModel.ATTR_KIND );
    attrTypes = attrManager.getNodeAttribute( AttributeModel.ATTR_TYPE );
    isMutable = attrManager.getNodeAttribute( AttributeModel.IS_MUTABLE ); 
    domain = attrManager.getNodeAttribute( AttributeModel.DOMAIN );
    isNodeAttr = attrManager.getNodeAttribute( AttributeModel.IS_NODE_ATTR ); 
    attrLabels = attrManager.getNodeAttribute( AttributeModel.ATTR_LABEL );

    rebuildModel();
    srcModel.addModelListener( srcModelBreakageHandler );
    finalizeInitialization();
  }
  
  
  
  //======================================================================
  //== AttributeModel convienence methods
  //======================================================================
  
  /**
   * Get the value of {@link #ATTR_KIND} attribute.
   */
  @Override
  public int getKind( final IRNode node )
  {
    // synchronization taken care of in the attribute wrapper via inheritance
    return (node.getSlotValue( attrKinds )).intValue();
  }
  
  /**
   * Get the value of {@link #ATTR_LABEL} attribute.
   */
  @Override
  public String getLabel( final IRNode node )
  {
    // synchronization taken care of in the attribute wrapper via inheritance
    return node.getSlotValue( attrLabels );
  }
  
  /**
   * Set the value of {@link #ATTR_LABEL} attribute.
   */
  @Override
  public void setLabel( final IRNode node, String label )
  {
    // synchronization taken care of in the attribute wrapper via inheritance
    // If this is inherited as mutable, the source model will break, causing
    // us to break and to send an event.
    node.setSlotValue( attrLabels, label );
  }
  
  /**
   * Get the value of {@link #ATTR_NAME} attribute.
   */
  @Override
  public String getName( final IRNode node )
  {
    // synchronization taken care of in the attribute wrapper via inheritance
    return node.getSlotValue( attrNames );
  }
  
  /**
   * Get the value of {@link #ATTR_TYPE} attribute.
   */
  @Override
  public IRType getType( final IRNode node )
  {
    // synchronization taken care of in the attribute wrapper via inheritance
    return node.getSlotValue( attrTypes );
  }
  
  /**
   * Get the value of {@link #IS_MUTABLE} attribute.
   */
  @Override
  public boolean isMutable( final IRNode node )
  {
    // synchronization taken care of in the attribute wrapper via inheritance
    final Boolean b = node.getSlotValue( isMutable );
    return b.booleanValue();
  }
  
  /**
   * Get the value of {@link #DOMAIN} attribute.
   */
  @Override
  public IRSequence getDomain( final IRNode node )
  {
    // synchronization taken care of in the attribute wrapper via inheritance
    return node.getSlotValue( domain );
  }

  /**
   * Get the value of {@link #IS_NODE_ATTR} attribute.
   */
  @Override
  public boolean isNodeAttr( final IRNode node )
  {
    // synchronization taken care of in the attribute wrapper via inheritance
    final Boolean b = node.getSlotValue( isNodeAttr );
    return b.booleanValue();
  }
  
  /**
   * Get pickled representation of the model's current state.
   */
  @Override
  public PickledAttributeModelState getPickledState()
  {
    // pass the buck to the source model
    return ((AttributeModel)srcModel).getPickledState();
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
  @Override
  public void setStateFromPickle(PickledAttributeModelState pickle)
  {
    // Pass the buck to the source model, which will break, which will
    // cause us to break (and send an event)
    ((AttributeModel)srcModel).setStateFromPickle( pickle );
  }
}
