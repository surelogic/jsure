// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/PredicateModelCore.java,v 1.23 2006/03/30 19:47:21 chance Exp $

package edu.cmu.cs.fluid.mvc.predicate;

import java.util.Iterator;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.sequence.SequenceModel;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.IntegerTable;

/**
 * Core implementation of the <code>PredicateModel</code> interface.
 * <p>Adds the model-level attribute {@link PredicateModel#PREDICATES_OF}.
 * <p>Adds the node-level attributes {@link PredicateModel#ATTR_NODE},
 * {@link PredicateModel#PREDICATE}, {@link PredicateModel#IS_VISIBLE},
 * {@link PredicateModel#IS_STYLED}, and {@link PredicateModel#ATTRIBUTE}.
 *
 * <p>The attributes {@link PredicateModel#IS_VISIBLE} and
 * {@link PredicateModel#IS_STYLED} can be changed from outside of the model.
 * The callback provided to the constructor must cause the model to send
 * break events when these attributes are chaged.
 *
 * @author Aaron Greenhouse
 */
public final class PredicateModelCore
extends AbstractCore
{
  //===========================================================
  //== Static Fields
  //===========================================================

  /** Reference to the enumeration type of the visible attribute */
  private static final IREnumeratedType visibleEnum =
    newEnumType( PredicateModel.VISIBLE_ENUM,
                 new String[] {
                       PredicateModel.PRED_VISIBLE,
                       PredicateModel.PRED_INVISIBLE,
                       PredicateModel.LEAVE_ALONE } );

  

  //===========================================================
  //== Fields
  //===========================================================

  /** Storage for the nodes associated with attributes. */
  private final SlotInfo<IRNode> attrNodes;

  /** Storage for predicates of attributes. */
  private final SlotInfo<AttributePredicate> predicates;

  /** Storage for the visibility attribute */
  private final SlotInfo<IREnumeratedType.Element> isVisible;

  /** Storage for the styled attribute */
  private final SlotInfo<Boolean> isStyled;

  /** Storage for the attributes in the model */
  private final SlotInfo<SlotInfo> attributes;

  /** Storage for the label attribute */
  private final SlotInfo<String> label;
  
  /** Storage for the PREDICATES_OF component-level attribute */
  private final ComponentSlot<Model> predicatesOf;

  
  
  
  public static IREnumeratedType getVisibleEnum()
  {
    return visibleEnum;
  }



  //===========================================================
  //== Constructor
  //===========================================================

  public PredicateModelCore(
    final String name, final SlotFactory sf, final Model model,
    final Object lock, final AttributeManager manager,
    final AttributeChangedCallback cb )
  throws SlotAlreadyRegisteredException
  {
    super( model, lock, manager );

    // Init model attributes
    predicatesOf = new SimpleComponentSlot<Model>( ModelType.prototype,
					    ConstantExplicitSlotFactory.prototype );
    attrManager.addCompAttribute(
      PredicateModel.PREDICATES_OF, Model.STRUCTURAL, predicatesOf );
             
    // Init node attributes
    attrNodes = sf.newAttribute( name + "-" + PredicateModel.ATTR_NODE,
				 IRNodeType.prototype );
    predicates = sf.newAttribute( name + "-" + PredicateModel.PREDICATE,
				  AttributePredicateType.prototype );

    // default to LEAVE ALONE
    int i = visibleEnum.getIndex(PredicateModel.LEAVE_ALONE);
    isVisible = sf.newAttribute( name + "-" + PredicateModel.IS_VISIBLE,
				 visibleEnum, visibleEnum.getElement(i) ); 
    isStyled = sf.newAttribute( name + "-" + PredicateModel.IS_STYLED,
				IRBooleanType.prototype, Boolean.FALSE );
    attributes = sf.newAttribute( name + "-" + PredicateModel.ATTRIBUTE,
				  new IRObjectType<SlotInfo>()); // XXX
    label    = sf.newAttribute( name + "-" + PredicateModel.PRED_LABEL,
				IRStringType.prototype, null );

    attrManager.addNodeAttribute(
      PredicateModel.ATTR_NODE, Model.INFORMATIONAL, attrNodes );
    attrManager.addNodeAttribute(
      PredicateModel.PREDICATE, Model.INFORMATIONAL, predicates );
    attrManager.addNodeAttribute(
      PredicateModel.IS_VISIBLE, Model.INFORMATIONAL, isVisible, cb );

    attrManager.addNodeAttribute(
      PredicateModel.IS_STYLED, Model.INFORMATIONAL, isStyled, cb );
    attrManager.addNodeAttribute(
      PredicateModel.ATTRIBUTE, Model.INFORMATIONAL, attributes );
    attrManager.addNodeAttribute(
      PredicateModel.PRED_LABEL, Model.INFORMATIONAL, label, cb );
  }



  //===========================================================
  //== Attribute Convienence methods  
  //===========================================================

  /**
   * Set the {@link PredicateModel#PREDICATES_OF} attribute
   */
  public void setPredicatesOf( final Model model )
  {
    predicatesOf.setValue( model );
  }

  /**
   * Get the value of {@link PredicateModel#ATTR_NODE}.
   */
  public IRNode getAttributeNode( final IRNode node )
  {
    return node.getSlotValue( attrNodes );
  }

  /**
   * Set the value of {@link PredicateModel#ATTR_NODE}.
   */
  public void setAttributeNode( final IRNode node, final IRNode attrNode )
  {
    node.setSlotValue( attrNodes, attrNode );
  }

  /**
   * Get the value of the {@link PredicateModel#PREDICATE} attribute.
   */
  public AttributePredicate getPredicate( final IRNode node )
  {
    return node.getSlotValue( predicates );
  }
  
  public void setPredicate( final IRNode node, final AttributePredicate p )
  {
    node.setSlotValue( predicates, p );
  }

  /**
   * Get the value of {@link PredicateModel#IS_VISIBLE} attribute.
   */
  public IREnumeratedType.Element isVisible( final IRNode node )
  {
    return node.getSlotValue( isVisible );
  }
  
  /**
   * Set the value of {@link PredicateModel#IS_VISIBLE} attribute.
   */
  public void setVisible( final IRNode node, final IREnumeratedType.Element vis )
  {
    if( visibleEnum != vis.getType() ) {
      throw new IllegalArgumentException( vis    
              + " is not a member of enumeration "
              + PredicateModel.VISIBLE_ENUM );
    }
    node.setSlotValue( isVisible, vis );
  }

  /**
   * Get the value of {@link PredicateModel#IS_STYLED} attribute.
   */
  public boolean isStyled( final IRNode node )
  {
    final Boolean b = node.getSlotValue( isStyled );
    return b.booleanValue();
  }

  /**
   * Set the value of {@link PredicateModel#IS_STYLED} attribute.
   */
  public void setStyled( final IRNode node, final boolean sty )
  {
    node.setSlotValue( isStyled, sty ? Boolean.TRUE : Boolean.FALSE );
  }

  /**
   * Get the value of {@link PredicateModel#ATTRIBUTE} attribute.
   */
  public SlotInfo getAttribute( final IRNode node )
  {
    return node.getSlotValue( attributes );
  }

  public void setAttribute( final IRNode node, final SlotInfo attr )
  {
    node.setSlotValue( attributes, attr );
  }

  /**
   * Get the value of {@link PredicateModel#PRED_LABEL} attribute.
   */
  public String getLabel( final IRNode node )
  {
    return node.getSlotValue( label );
  }

  /**
   * Set the value of {@link PredicateModel#PRED_LABEL} attribute.
   */
  public void setLabel( final IRNode node, final String l )
  {
    node.setSlotValue( label, l );
  }

  /**
   * Implementation delegate for {@link Model#nodeValueToString}; treats
   * the {@link PredicateModel#ATTR_NODE} value specially, by prepending
   * the name of the attribute to the IRNode value representation.
   */
  public String nodeValueToString(
    final ModelCore modelCore, final Model srcModel,
    final IRNode node, final String attr )
  throws UnknownAttributeException
  {
    if( attr == PredicateModel.ATTR_NODE ) {
      final String value = modelCore.nodeValueToString( node, attr );
      final String prefix =
        srcModel.getAttributeName(
          (IRNode) node.getSlotValue( partOf.getNodeAttribute( attr ) ) );
      return (prefix + " [" + value + "]");
    } else {
      return modelCore.nodeValueToString( node, attr );
    }
  }



  //===========================================================
  //== AttributeViewCore Factory Interfaces/Classes
  //===========================================================

  public static interface Factory 
  {
    public PredicateModelCore create(
      String name, Model model, Object lock, AttributeManager manager,
      AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException;
  }
  
  public static class StandardFactory
  implements Factory
  {
    private final SlotFactory factory;

    public StandardFactory( final SlotFactory sf )
    {
      factory = sf;
    }

    @Override
    public PredicateModelCore create(
      final String name, final Model model, final Object lock,
      final AttributeManager manager, final AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException
    {
      return new PredicateModelCore( name, factory, model, lock, manager, cb );
    }
  }
  


  //===========================================================
  //== Pickled State methods
  //===========================================================

  /**
   * Get pickled representation of the model's current state.
   */
  public PickledPredicateModelState getPickledState()
  {
    return new PickledPredicateModelState( partOf );
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
  public void setStateFromPickle( final PickledPredicateModelState pickle )
  {
    if( pickle.getModel().equals( partOf.getNode() ) ) {
      final Iterator nodes = pickle.getAttributes();
      final SlotInfo<Integer> index = attrManager.getNodeAttribute( SequenceModel.INDEX );

      int currentLoc = 0;
      while( nodes.hasNext() ) {
        final PickledPredicateModelState.PredState predState =
          (PickledPredicateModelState.PredState)nodes.next();
        final IRNode node = predState.predNode;

        // ignore attributes that are no longer in the model
        if( partOf.isPresent( node ) ) {
          node.setSlotValue( index, IntegerTable.newInteger( currentLoc ) );
          node.setSlotValue( isVisible, predState.isVisible );
          node.setSlotValue( isStyled, predState.isStyled );
          currentLoc += 1;
        }
      }
    } else {
      throw new IllegalArgumentException( "Pickled state is from a different model" );
    }
  }
}

