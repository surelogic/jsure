/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/AttributeBasedPredicateVisibilityViewImpl.java,v 1.11 2006/03/30 16:20:26 chance Exp $
 *
 * AttributeBasedPredicateVisibilityViewImpl.java
 * Created on March 19, 2002, 3:46 PM
 */

package edu.cmu.cs.fluid.mvc.visibility;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.attr.AttributeModel;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.ir.*;

/**
 * Minimal implementation of {@link AttributeBasedPredicateVisibilityView}.
 *
 * @author Aaron Greenhouse
 */
final class AttributeBasedPredicateVisibilityViewImpl
extends AbstractModelToVisibilityStatefulView
implements AttributeBasedPredicateVisibilityView
{
  //===========================================================
  //== Fields
  //===========================================================
  
  /** Reference to the attribute model used to control node visibility. */
  private final AttributeModel srcAttrModel;
  
  /** Storage for the displayed attribute */
  private final SlotInfo<Boolean> isDisplayed;

  /**
   * Local cache of visible attributes: IRNode -> Boolean.
   * Based on the assumption that the AttributeModel will change less
   * frequently than the PredicateModel.
   */
  private final Map<IRNode,Boolean> vizAttrs = new HashMap<IRNode,Boolean>();

  
  
  //===========================================================
  //== Constructor
  //===========================================================

  public AttributeBasedPredicateVisibilityViewImpl(
    final String name, final PredicateModel srcModel,
    final AttributeModel attrModel,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final VisibilityModelCore.Factory vmf )
  throws SlotAlreadyRegisteredException
  {
    /*
     * XXX: Warning!  Possible dependence problem here for attributes of this
     * XXX: model because the isAttributable attribute is being defined by
     * XXX: state of *this* model.
     */
    super( name, mf, vf, vmf, srcModel );
    srcAttrModel = attrModel;
    
    /* Initialize Model-level attributes */
    final IRSequence<Model> srcModels =
      ConstantSlotFactory.prototype.newSequence(2);
    srcModels.setElementAt( srcModel, 0 );
    srcModels.setElementAt( srcAttrModel, 1 );
    viewCore.setSourceModels( srcModels );

    /* init node-level attributes */
    final SlotInfo<Boolean> isVisible = 
      SimpleSlotFactory.prototype.newAttribute(
        name + "-" + VisibilityModel.IS_VISIBLE, IRBooleanType.prototype );
    attrManager.addNodeAttribute(
      VisibilityModel.IS_VISIBLE, Model.STRUCTURAL, isVisible );
    visModCore.setIsVisibleAttribute( isVisible );

    isDisplayed = SimpleSlotFactory.prototype.newAttribute(
      name + "-" + IS_DISPLAYED, IRBooleanType.prototype,  Boolean.FALSE ); 
    attrManager.addNodeAttribute( 
      IS_DISPLAYED, Model.INFORMATIONAL, true, new Model[] { srcAttrModel },
      isDisplayed, new IsDisplayChangedCallback() );
    
    /* init model state */
    rebuildModel();
    
    /* 
     * Add listeners:
     *  - To the source (predicate model) in case new nodes are added, etc.
     *  - To the attribute model in case the rules are changed, etc.
     *
     * This may cause unneeded double rebuilds, but we will see how it works
     * in practice.
     */
    srcModel.addModelListener( srcModelBreakageHandler );
    srcAttrModel.addModelListener( srcModelBreakageHandler );
    finalizeInitialization();
  }

  
  
  //===========================================================
  //== Callbacks
  //===========================================================

  /**
   * Attribute changed callback for catching changes to the 
   * {@link AttributeBasedPredicateVisibilityView#IS_DISPLAYED} attribute.
   * Caused the model to rebuild.
   */
  private class IsDisplayChangedCallback
  extends AbstractAttributeChangedCallback
  {
    @Override
    protected void attributeChangedImpl(
      final String attr, final IRNode node, final Object val )
    {
      if( (attr == IS_DISPLAYED) ) {
        signalBreak( new AttributeValuesChangedEvent(
                           AttributeBasedPredicateVisibilityViewImpl.this,
                           node, attr, val ) );
      }
    }
  }

  
  
  //===========================================================
  //== Rebuild methods
  //===========================================================

  /**
   * This causes the source model to be traversed and the
   * sub-model to be built.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void rebuildModel( final List events ) throws InterruptedException
  {
    synchronized( structLock ) {
      updateAttrCache( events );
      
      final SlotInfo<IRNode> attrNodes =
        srcModel.getNodeAttribute( PredicateModel.ATTR_NODE );
      
      for( final Iterator<IRNode> i = srcModel.getNodes(); i.hasNext(); ) {
        final IRNode n  = i.next(); 
        final Boolean b = vizAttrs.get( n.getSlotValue( attrNodes ) );
        if (b == null) {
          // (new Throwable("On "+n)).printStackTrace();
        }
        visModCore.setVisible( n, b );
      }
    }

    // Break our views
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  /**
   * Update the set of visible attributes if we have events that come
   * from the AttributeModel.  Based on the assumption that the AttributeModel
   * will change less frequently than the PredicateModel.
   */
  private void updateAttrCache( final List events )
  {
    /* 
     * Should be smart about this and use AttributeChangedEvents if they
     * are available, but I suspect processing of them may be slower than
     * just redoing the whole thing.
     *
     * Also rebuild if the list of events has a size of zero.  
     */
    final boolean rebuildCache = true;
    /*
    boolean rebuildCache = (events.size() == 0);
    for( final Iterator i = events.iterator(); !rebuildCache && i.hasNext(); ) {
      final ModelEvent e = (ModelEvent) i.next();
      rebuildCache = (e.getSource() == srcAttrModel);
    }
    */
    
    if( rebuildCache ) {
      vizAttrs.clear();
      for( final Iterator<IRNode> i = srcAttrModel.getNodes(); i.hasNext(); ) {
    	final IRNode n  = i.next(); 
        vizAttrs.put( n, n.getSlotValue( isDisplayed ) );
      }
    }
  }
  
  
  //===========================================================
  //== Convienence methods
  //===========================================================

  // inherit javadoc
  @Override
  public void setDisplayed( final IRNode node, final boolean val )
  {
    final Boolean v = val ? Boolean.TRUE : Boolean.FALSE;
    synchronized( structLock ) {
      node.setSlotValue( isDisplayed, v );      
    }
    signalBreak(
      new AttributeValuesChangedEvent( this, node, IS_DISPLAYED, v ) );
  }
  
  // inherit javadoc
  @Override
  public boolean isDisplayed( final IRNode node )
  {
    synchronized( structLock ) {
      return node.getSlotValue( isDisplayed ).booleanValue();
    }
  }

  /**
   * Implementation delegate for {@link Model#nodeValueToString}; treats
   * the {@link PredicateModel#ATTR_NODE} value specially, by prepending
   * the name of the attribute to the IRNode value representation.
   */
  @SuppressWarnings("unchecked")
  @Override
  public String idNode( final IRNode node )
  throws UnknownAttributeException
  {
    final String id = srcModel.idNode( node );
    final IRNode attrNode =
      (IRNode) node.getSlotValue(
                 srcModel.getNodeAttribute( PredicateModel.ATTR_NODE ) );
    final String attrName = srcAttrModel.getLabel( attrNode );
    return attrName + " (" + id + ")";
  }
}
