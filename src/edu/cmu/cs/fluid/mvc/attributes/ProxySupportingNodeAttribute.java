/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attributes/ProxySupportingNodeAttribute.java,v 1.8 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.attributes;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * Wrapper for node attributes that need to store proxy node attribute
 * values.  Special local storage is used to allow proxy node
 * values to be set even if the attribute is otherwise immutable.
 * The attribute does not allow the attribute values for proxy
 * nodes to be set.  The values for proxy nodes can only
 * be set by direct manipulation of the provided
 * local storage slot info from within the appropriate model implementation.
 *
 * <p>The attribute must only be used with a model that contains the
 * {@link ConfigurableView#IS_PROXY} attribute.  This is checked by the 
 * constructur.
 *
 * <p><em>This wrapper must always be wrapped by another
 * wrapper that checks the <code>isAttributable</code> value.</em>
 */
public final class ProxySupportingNodeAttribute<T>
extends SlotInfoWrapper<T>
{
  /** The storage used for the proxy values. */
  private final SlotInfo<T> proxyValues;

  /**
   * Reference to the {@link ConfigurableView#IS_PROXY} attribute of the 
   * model this attribute is a part of.
   */
  private final SlotInfo<Boolean> isProxyNodeAttr;
  
  
  
  /**
   * Create a new attribute that supports proxy nodes.
   * @param normalValues The attribute storage to wrap.
   * @param proxyValues The attribute storage for proxy nodes.
   * @param model The model the attribute is a part of.
   * @exception IllegalArgumentException Thrown if the given model does not 
   * support the node attribute {@link ConfigurableView#IS_PROXY}.
   */
  @SuppressWarnings("unchecked")
  public ProxySupportingNodeAttribute(
    final SlotInfo<T> normalValues, final SlotInfo<T> proxyValues, final Model model )
  {
    super( normalValues );
    this.proxyValues = proxyValues;
    
    try {
      isProxyNodeAttr = model.getNodeAttribute( ConfigurableView.IS_PROXY );
    } catch( final UnknownAttributeException e ) {
      throw new IllegalArgumentException(
                  "The provided model does not have the IS_PROXY attribute." );
    }
  }

  /**
   * Convienence method to test if a node is a proxy node.
   */
  private boolean isProxyNode( final IRNode node ) 
  {
    return (node.getSlotValue( isProxyNodeAttr )).booleanValue();
  }
  
  /**
   * The value exists if the node is a proxy node with a value or if the
   * node's value exists in the wrapped attribute value storage.
   */
  @Override
  protected boolean valueExists( final IRNode node )
  {
    // Don't need to check isAttributable() because this wrapper
    // will always be inside of another wrapper that does.
    return node.valueExists( proxyValues ) || node.valueExists( wrapped );
  }

  /**
   * If the node is a proxy node return the local value, otherwise 
   * defer to the wrapped attribute.
   */
  @Override
  protected T getSlotValue( final IRNode node )
  throws SlotUndefinedException
  {
    // Don't need to check isAttributable() because this wrapper
    // will always be inside of another wrapper that does.
    if( isProxyNode( node ) ) {
      return node.getSlotValue( proxyValues );
    } else {
      return super.getSlotValue( node );
    }
  }

  /**
   * Throw an exception if node is a proxy node; deleaget to the wrapped
   * attribute otherwise.
   * @exception SlotImmutableException Thrown if <code>node</code> is a 
   * proxy node; may also be thrown by the wrapped attribute if the 
   * wrapped attribute is immutable.
   */
  @Override
  protected void setSlotValue( final IRNode node, final T newValue )
  throws SlotImmutableException
  {
    if( isProxyNode( node ) ) {
      throw new SlotImmutableException( "Cannot set values for proxy nodes." );
    } else {
      super.setSlotValue( node, newValue );
    }
  }
}
