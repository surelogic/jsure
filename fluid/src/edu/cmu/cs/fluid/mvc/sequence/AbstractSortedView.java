/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/AbstractSortedView.java,v 1.11 2007/01/12 18:53:28 chance Exp $
 *
 * AbstractSortedView.java
 * Created on March 7, 2002, 3:32 PM
 */

package edu.cmu.cs.fluid.mvc.sequence;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.util.StringSet;
import edu.cmu.cs.fluid.ir.*;

/**
 * Minimum abstract implementation of {@link SortedView}.
 * (include list of which attributes are initialized or not)
 *
 * @author Aaron Greenhouse
 */
public abstract class AbstractSortedView
extends AbstractModelToSequenceStatefulView
{
  /** The set model being viewed. */
  protected final Model srcModel;

  /** Storage for the {@link SortedView#IS_ASCENDING} attribute. */
  private final ComponentSlot<Boolean> isAscending;
  
  /** Storage for the {@link SortedView#SORT_ATTR} attribute. */
  private final ComponentSlot<String> sortAttr;
  
  /**
   * Sneaky duplicate reference to the sort attribute as a String.
   * Used to avoid the overhead of getting the value out of the
   * ComponentSlot during the sorting process.  This shadow reference
   * is maintained by the convienence method and by an attribute changed
   * callback that also does the error checking.
   */
  private String sortAttrShadow;
  
  /**
   * Sneaky duplicate reference to the ascending attribute as a boolean.
   * Used to avoid the overhead of getting the value out of the
   * ComponentSlot during the sorting process.  This shadow reference
   * is maintained by the convienence method and by an attribute changed
   * callback.
   */
  private boolean isAscendingShadow;
 
  /** 
   * The attribute inheritence policy.  Need to keep a reference to it to deal
   * with dynamically added attributes.
   */
  private final AttributeInheritancePolicy attrPolicy;
  
  /**
   * Local cache of the acceptable values for the sort attribute.
   */
  private final Set<String> acceptableAttrs;

  /**
   * The comparator used to define the total ordering of nodes
   * based on the sort attribute and ascending values.
   */
  private final Comparator<IRNode> totalOrder;
  

  
  // Checks for legitimacy of sortAttr
  public AbstractSortedView(
    final String name, final Model src, final ModelCore.Factory mf,
    final ViewCore.Factory vf, final SequenceModelCore.Factory smf,
    final AttributeInheritancePolicy policy, final String attr,
    final boolean isAsc )
  throws SlotAlreadyRegisteredException
  {
    super( name, mf, vf, smf, 
           LocalAttributeManagerFactory.prototype,
           BasicAttributeInheritanceManagerFactory.prototype );
    
    /* init the source model stuff */
    srcModel = src; // local copy of src mode reference
    final IRSequence<Model> srcModels = ConstantSlotFactory.prototype.newSequence( 1 );
    srcModels.setElementAt( srcModel, 0 );
    viewCore.setSourceModels( srcModels );

    /* init the local storage for the sort attributes. */
    isAscending = SimpleComponentSlotFactory.simplePrototype.predefinedSlot(
                    IRBooleanType.prototype, isAsc ? Boolean.TRUE : Boolean.FALSE );
    isAscendingShadow = isAsc;
    sortAttr = SimpleComponentSlotFactory.simplePrototype.undefinedSlot(
                 IRStringType.prototype );
    totalOrder = new AttributeBasedTotalOrder();
    
    /* Add the sort attributes to the attribute manager */
    final AttributeChangedCallback localCB = new LocalAttributesCB();
    attrManager.addCompAttribute(
      SortedView.IS_ASCENDING, Model.STRUCTURAL, true, isAscending, localCB );
    attrManager.addCompAttribute(
      SortedView.SORT_ATTR, Model.STRUCTURAL, true, sortAttr, localCB );

    /* 
     * Inherit attributes and init the cache of the acceptable attributes
     * for sorting.  Also throw an exception if the provided sort attribute
     * is not acceptable.
     */
    attrPolicy = policy;
    inheritManager.inheritAttributesFromModel( src, policy, AttributeChangedCallback.nullCallback );
    acceptableAttrs = new StringSet();
    cacheAcceptableAttrs();
    
    if( !isAcceptableSortAttr( attr ) ) {
      throw new IllegalArgumentException(
                "Attribute \"" + attr
              + "\" is not a node attribute inherited from the source model." );
    }
    sortAttr.setValue( attr );
    sortAttrShadow = attr;
  }

  
  
  private void cacheAcceptableAttrs()
  {
    final AttributeInheritancePolicy.HowToInherit[] which = 
      attrPolicy.nodeAttrsToInherit( srcModel );
    for( int i = 0; i < which.length; i++ ) {
      if(    (which[i].mode == AttributeInheritanceManager.IMMUTABLE)
          || (which[i].mode == AttributeInheritanceManager.MUTABLE_SOURCE) ) {
        acceptableAttrs.add( which[i].attr );
      }
    }
  }
  
  
  /**
   * Check whether <code>attrName</code> names a node attribute in the
   * source model that is inherited as either immutable or 
   * mutable-source by this model.
   */
  private boolean isAcceptableSortAttr( final String attrName )
  {
    return acceptableAttrs.contains( attrName );
  }
  
  /**
   * Generate the message for the IllegalArgumentException thrown
   * when an attribute is unacceptable for the {@link SortedView#SORT_ATTR}
   * attribute.
   */
  private String generateExceptionMsg( final String attrName )
  {
    final StringBuilder buf = new StringBuilder( "Attribute \"" );
    buf.append( attrName );
    buf.append( "\" is not a node attribute inherited as immutable from the source model." );
    return buf.toString();
  }
  
  /**
   * Convienence method for setting the model-level attribute
   * {@link SortedView#SORT_ATTR}.
   * @exception IllegalArgumentException Thrown if the given
   *  attribute is not a node-level attribute of the source model that has
   *  been inherited by the sorted view as immutable or mutable source.
   */
  public final void setSortAttribute( final String attrName )
  {
    synchronized( structLock ) {
      if( isAcceptableSortAttr( attrName ) ) {
        sortAttr.setValue( attrName );
        sortAttrShadow = attrName;
      } else {
        throw new IllegalArgumentException( generateExceptionMsg( attrName ) );
      }      
    }
    signalBreak( new AttributeValuesChangedEvent( this, SortedView.SORT_ATTR, attrName ) );          
  }
  
  public final String getSortAttribute()
  {
    synchronized( structLock ) {
      return sortAttrShadow;
    }
  }  

  public final boolean isAscending()
  {
    synchronized( structLock ) {
      return isAscendingShadow;
    }
  }
  
  public final void setAscending( final boolean isAsc )
  {
    final Boolean val = isAsc ? Boolean.TRUE : Boolean.FALSE;
    synchronized( structLock ) {
      isAscending.setValue( val );
      isAscendingShadow = isAsc;
    }
    signalBreak( new AttributeValuesChangedEvent( this, SortedView.SORT_ATTR, val ) );          
  }



  private class LocalAttributesCB
  extends AbstractAttributeChangedCallback
  {
    @Override
    protected void attributeChangedImpl(
      final String attr, final IRNode node, final Object val )
    {
      if( attr == SortedView.SORT_ATTR ) {
        synchronized( structLock ) {
          if( isAcceptableSortAttr( (String)val ) ) {
            sortAttrShadow = (String)val;
          } else {
            // reset the value to the old one.
            sortAttr.setValue( sortAttrShadow );
            throw new IllegalArgumentException( generateExceptionMsg( (String)val ) );
          }
        }
        signalBreak( new AttributeValuesChangedEvent(
                           AbstractSortedView.this, SortedView.SORT_ATTR, val ) );          
      } else if( attr == SortedView.IS_ASCENDING ) {
        isAscendingShadow = ((Boolean)val).booleanValue();
        signalBreak( new AttributeValuesChangedEvent(
                           AbstractSortedView.this, SortedView.IS_ASCENDING, val ) );
      }
    }
  }

  
  
  /**
   * Comparator based on the {@link AbstractSortedView#sortAttrShadow}
   * and {@link AbstractSortedView#isAscendingShadow} fields.
   */
  private final class AttributeBasedTotalOrder
  implements Comparator<IRNode>
  {
    @Override
    public int compare( final IRNode o1, final IRNode o2 )
    {
      final String v1 = srcModel.nodeValueToString( o1, sortAttrShadow );
      final String v2 = srcModel.nodeValueToString( o2, sortAttrShadow );
      return isAscendingShadow
             ? v1.compareTo( v2 )
             : v2.compareTo( v1 );  
    }
  }
  
  
  
  /**
   * Invoked when a new attribute is added to the source model.  In this case
   * we try to inherit the attribute.
   */
  @Override
  protected final void attributeAddedToSource(
    final Model src, final String attr, final boolean isNodeLevel )
  {
    AttributeInheritancePolicy.HowToInherit[] which;
    if( isNodeLevel ) {
      which = attrPolicy.nodeAttrsToInherit( src );
    } else {
      which = attrPolicy.compAttrsToInherit( src );
    }
    
    AttributeInheritancePolicy.HowToInherit inherit = null;
    for( int i = 0; (inherit == null) && (i < which.length); i++ ) {
      if( which[i].attr == attr ) inherit = which[i];
    }
    
    if( inherit != null ) {
      synchronized( structLock ) {
        if( isNodeLevel ) {
          inheritManager.inheritNodeAttribute(
            src, attr, inherit.inheritAs, inherit.mode,
            inherit.kind, AttributeChangedCallback.nullCallback );
          acceptableAttrs.clear();
          cacheAcceptableAttrs();
        } else {
          inheritManager.inheritCompAttribute(
            src, attr, inherit.inheritAs, inherit.mode,
            inherit.kind, AttributeChangedCallback.nullCallback );
        }
        
      }
      
      /* 
       * XXX This is broken because this model will not inform its children
       * XXX of any new attributes.  The event handling/rebuild stuff needs
       * XXX to be globally redone for this to be dealt with properly.  It can
       * XXX be done with the current system, but it is not easy to do.
       */
      signalBreak( new ModelEvent( src ) );
    }
  }
  
  
  @Override
  protected final void rebuildModel( final List<ModelEvent> events )
  {
    // ought to protect against new attributes being added during 
    // a rebuild...

    // clear the model, and rebuild using an insertion sort.
    synchronized( structLock ) {
      seqModCore.clearModel();
      seqModCore.buildSorted( srcModel.getNodes(), totalOrder );
    }
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }
}
