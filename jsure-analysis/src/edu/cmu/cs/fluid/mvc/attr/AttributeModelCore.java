// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/AttributeModelCore.java,v 1.13 2006/03/29 18:30:56 chance Exp $

package edu.cmu.cs.fluid.mvc.attr;

import java.util.Iterator;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.set.SetModelCore;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.IntegerTable;

/**
 * Core implementation of the <code>AttributeModel</code> interface.
 * <p>Adds the model-level attribute {@link AttributeModel#ATTRIBUTES_OF}.
 * <p>Adds the node-level attributes {@link AttributeModel#ATTR_NAME},
 * {@link AttributeModel#ATTR_LABEL}, {@link AttributeModel#ATTR_TYPE},
 * {@link AttributeModel#ATTR_KIND}, {@link AttributeModel#DOMAIN},
 * {@link AttributeModel#IS_MUTABLE}, {@link AttributeModel#IS_NODE_ATTR}.
 *
 * <p>The attribute {@link AttributeModel#ATTR_LABEL}
 * can be changed from outside of the model.
 * The callback provided to the constructor must cause the model to send
 * break events when these attributes are chaged.
 *
 * @author Edwin Chan
 * @author Aaron Greenhouse
 */
public final class AttributeModelCore
extends AbstractCore
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

  /** Storage from the domain attribute */
  private final SlotInfo<IRSequence<Model>> domain;
  
  /** Storage for the mutable attribute */
  private final SlotInfo<Boolean> isMutable;

  /** Storage for the node attr attribute */
  private final SlotInfo<Boolean> isNodeAttr;

  // /** Storage for the displayed attribute */
  // private final SlotInfo isDisplayed;

  /** Storage for the source model. */
  private final ComponentSlot<Model> attrsOf;



  //===========================================================
  //== Constructor
  //===========================================================

  public AttributeModelCore(
    final String name, final SlotFactory sf, final Model model,
    final Object lock, final AttributeManager manager,
    final AttributeChangedCallback cb )
  throws SlotAlreadyRegisteredException
  {
    super( model, lock, manager );

    // Init model attributes
    attrsOf = SimpleComponentSlotFactory.constantPrototype.undefinedSlot(
                ModelType.prototype );
    attrManager.addCompAttribute(
      AttributeModel.ATTRIBUTES_OF, Model.STRUCTURAL, attrsOf );
             
    // Init node attributes
    attrNames = sf.newAttribute(
                  name + "-" + AttributeModel.ATTR_NAME, IRStringType.prototype ); 
    attrManager.addNodeAttribute(
      AttributeModel.ATTR_NAME, Model.INFORMATIONAL, attrNames );

    attrKinds = sf.newAttribute(
                  name + "-" + AttributeModel.ATTR_KIND, IRIntegerType.prototype );
    attrManager.addNodeAttribute(
      AttributeModel.ATTR_KIND, Model.INFORMATIONAL, attrKinds );

    attrTypes = sf.newAttribute(
                  name + "-" + AttributeModel.ATTR_TYPE, IRTypeType.prototype );
    attrManager.addNodeAttribute(
      AttributeModel.ATTR_TYPE, Model.INFORMATIONAL, attrTypes );

    domain = sf.newAttribute( name + "-" + AttributeModel.DOMAIN,
                              new IRSequenceType<Model>( ModelType.prototype ) );
    attrManager.addNodeAttribute(
      AttributeModel.DOMAIN, Model.INFORMATIONAL, domain );
    
    isMutable = sf.newAttribute(
                  name + "-" + AttributeModel.IS_MUTABLE, IRBooleanType.prototype );
    attrManager.addNodeAttribute(
      AttributeModel.IS_MUTABLE, Model.INFORMATIONAL, isMutable ); 

    isNodeAttr = sf.newAttribute(
                   name + "-" + AttributeModel.IS_NODE_ATTR, IRBooleanType.prototype );
    attrManager.addNodeAttribute(
      AttributeModel.IS_NODE_ATTR, Model.INFORMATIONAL, isNodeAttr ); 

    attrLabels = sf.newAttribute(
      name + "-" + AttributeModel.ATTR_LABEL, IRStringType.prototype ); 
    attrManager.addNodeAttribute(
      AttributeModel.ATTR_LABEL, Model.INFORMATIONAL, attrLabels, cb );
  }



  //===========================================================
  //== Attribute Convienence methods  
  //===========================================================

  public void setAttributesOf( final Model m )
  {
    attrsOf.setValue( m );
  }

  public Model getAttributesOf()
  {
    return attrsOf.getValue();
  }

  /**
   * Get the value of {@link AttributeModel#ATTR_NAME} attribute.
   */
  public String getName( final IRNode node ) {
    return node.getSlotValue( attrNames );
  }

  public void setName( final IRNode node, final String name ) {
    node.setSlotValue( attrNames, name );
  }

  /**
   * Get the value of {@link AttributeModel#ATTR_LABEL} attribute.
   */
  public String getLabel( final IRNode node ) {
    return node.getSlotValue( attrLabels );
  }

  public void setLabel( final IRNode node, final String label ) {
    node.setSlotValue( attrLabels, label );
  }


  public boolean existsLabel( final IRNode node ) {
    return node.valueExists( attrLabels );
  }

  public static String generateStandardLabel(final String name) {
    int dot = name.lastIndexOf('.');
    return (dot < 0) ? name : name.substring(dot+1);
  }

  /**
   * Get the value of {@link AttributeModel#ATTR_TYPE} attribute.  
   */
  public IRType getType( final IRNode node ) {
    return node.getSlotValue( attrTypes );
  }

  public void setType( final IRNode node, final IRType type ) {
    node.setSlotValue( attrTypes, type );
  }

  /**
   * Get the value of {@link AttributeModel#ATTR_TYPE} attribute.  
   */
  public int getKind( final IRNode node ) {
    return (node.getSlotValue( attrKinds )).intValue();
  }

  public void setKind( final IRNode node, final int kind ) {
    node.setSlotValue( attrKinds, IntegerTable.newInteger( kind ) );
  }

  /**
   * Get the value of {@link AttributeModel#DOMAIN} attribute.
   */
  public IRSequence<Model> getDomain( final IRNode node )
  {
    return (node.getSlotValue( domain ));
  }
  
  /**
   * Get the value of {@link AttributeModel#IS_MUTABLE} attribute.
   */
  public boolean isMutable( final IRNode node ) {
    final Boolean b = node.getSlotValue( isMutable );
    return b.booleanValue();
  }

  /**
   * Set the value of {@link AttributeModel#IS_MUTABLE} attribute.
   */
  public void setMutable( final IRNode node, final boolean mutable ) {
    node.setSlotValue( isMutable, mutable ? Boolean.TRUE : Boolean.FALSE );
  }

  /**
   * Get the value of {@link AttributeModel#IS_NODE_ATTR} attribute.
   */
  public boolean isNodeAttr( final IRNode node ) {
    final Boolean b = node.getSlotValue( isNodeAttr );
    return b.booleanValue();
  }

  /**
   * Set the value of {@link AttributeModel#IS_NODE_ATTR} attribute.
   */
  public void setNodeAttr( final IRNode node, final boolean nodeAttr ) {
    node.setSlotValue( isNodeAttr, nodeAttr ? Boolean.TRUE : Boolean.FALSE );
  }

  
  
  //===========================================================
  //== Model maintenance convience methods
  //===========================================================

  /**
   * Add the node representing an attribute to the model,
   * initializing the {@link AttributeModel#ATTR_NAME}, and
   * {@link AttributeModel#ATTR_LABEL} attributes only.
   * @param setModCore The SetModelCore being used to maintain the structure
   *    of the AttributeModel.
   * @param node The IRNode representing the attribute.
   * @param attrName The name of the attribute.
   */
  private void addAttribute(
    final SetModelCore setModCore, final IRNode node, final String attrName )
  {
    setModCore.addNode( node );
    node.setSlotValue( attrNames, attrName );
    if( !node.valueExists( attrLabels ) ) {
      node.setSlotValue( attrLabels, generateStandardLabel( attrName ) );
    }
  }
  
  /**
   * Add the node representing a node-level attribute to the 
   * model and intialize it's attributes.
   * @param srcModel The model whose attributes are being modeled.
   * @param setModCore The SetModelCore being used to maintain the structure
   *    of the AttributeModel.
   * @param attrName The name of the attribute.
   */
  public void initNodeAttribute(
    final Model srcModel, final SetModelCore setModCore, final String attrName )
  {
    final IRNode node = srcModel.getNodeAttrNode( attrName );
    addAttribute( setModCore, node, attrName );
    node.setSlotValue( isNodeAttr, Boolean.TRUE );
    node.setSlotValue( attrTypes, srcModel.getNodeAttribute( attrName ).getType() );
    node.setSlotValue( attrKinds, srcModel.getNodeAttrKind( attrName ) );
    initDomainValue( srcModel, attrName, node );
    node.setSlotValue(
      isMutable, 
      (srcModel.isNodeAttrMutable( attrName ) ? Boolean.TRUE : Boolean.FALSE) );
  }

  private void initDomainValue( final Model srcModel, final String attr, final IRNode node )
  {
    IRSequence<Model> seq = null;
    if( srcModel.getNodeAttrDomain( attr ) == Model.MODEL_DOMAIN ) {
      seq = ConstantSlotFactory.prototype.newSequence(1);
      seq.setElementAt( srcModel, 0 );
    } else {
      final Model[] srcs = srcModel.getNodeAttrDomainSrcs( attr );
      seq = ConstantSlotFactory.prototype.newSequence( srcs.length );
      for( int i = 0; i < srcs.length; i++ ) {
        seq.setElementAt( srcs[i], i );
      }      
    }
    node.setSlotValue( domain, seq );
  }
  
  /**
   * Add the node representing a model-level attribute to the 
   * model and intialize it's attributes.
   * @param srcModel The model whose attributes are being modeled.
   * @param setModCore The SetModelCore being used to maintain the structure
   *    of the AttributeModel.
   * @param attrName The name of the attribute.
   */
  public void initCompAttribute(
    final Model srcModel, final SetModelCore setModCore, final String attrName )
  {
    final IRNode node = srcModel.getCompAttrNode( attrName );
    addAttribute( setModCore, node, attrName );
    node.setSlotValue( isNodeAttr, Boolean.FALSE );
    node.setSlotValue( attrTypes, srcModel.getCompAttribute( attrName ).getType() );
    node.setSlotValue( attrKinds, srcModel.getCompAttrKind( attrName ) );
    node.setSlotValue(
      isMutable, 
      (srcModel.isCompAttrMutable( attrName ) ? Boolean.TRUE : Boolean.FALSE));
  }

  
  
  //===========================================================
  //== Pickled State methods
  //===========================================================

  /**
   * Get pickled representation of the model's current state.
   */
  public PickledAttributeModelState getPickledState()
  {
    return new PickledAttributeModelState( partOf );
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
  public void setStateFromPickle( final PickledAttributeModelState pickle )
  {
    if( pickle.getModel().equals( partOf.getNode() ) ) {
      final Iterator<PickledAttributeModelState.AttrState> nodes = pickle.getAttributes();

      while( nodes.hasNext() ) {
        final PickledAttributeModelState.AttrState attrState = nodes.next();
        final IRNode node = attrState.attrNode;

        // ignore attributes that are no longer in the model
        if( partOf.isPresent( node ) ) {
          node.setSlotValue( attrLabels, attrState.label );
        }
      }
    } else {
      throw new IllegalArgumentException( "Pickled state is from a different model" );
    }
  }



  //===========================================================
  //== AttributeViewCore Factory Interfaces/Classes
  //===========================================================

  public static interface Factory 
  {
    public AttributeModelCore create(
      String name, Model model, Object lock, AttributeManager manager,
      AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException;
  }
  
  public final static class StandardFactory
  implements Factory
  {
    private final SlotFactory factory;

    public StandardFactory( final SlotFactory sf )
    {
      factory = sf;
    }

    @Override
    public AttributeModelCore create(
      final String name, final Model model, final Object lock,
      final AttributeManager manager, final AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException
    {
      return new AttributeModelCore( name, factory, model, lock, manager, cb );
    }
  }
}

