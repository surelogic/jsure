/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attributes/PhantomSupportingNodeAttribute.java,v 1.10 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.attributes;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * A component slot wrapper that handles "phantom" nodes.  Phantom nodes
 * originate in difference models and represent places where something was.
 * In terms of attribute values, they are distinguished because they map
 * their values back to the node that represents where that something currently
 * is.  A {@link PhantomNodeIdentifier} object is used to define the mapping.
 * This wrapper also makes the attribute immutable.
 * 
 * @author Aaron Greenhouse
 */
public final class PhantomSupportingNodeAttribute<T>
extends SlotInfo<T>
{
  /** The lock to use to protect that attribute value. */
  private final Object structLock;
  
  /**
   * Delegate the real work to an underlying {@link GuardedImmutableNodeAttribute}
   * instance.  We'll just handle the phantom node mapping issues.
   */
  private final SlotInfo<T> delegate;
  
  /**
   * Helper for identifying and mapping phantom nodes.
   */
  private final PhantomNodeIdentifier helper;
  
  

  /**
   * Create a new phantom node-supporting attribute.
   * @param model The model the attribute is a part of.
   * @param mutex The lock to use to protect the attribute's values.
   * @param attr The underlying value storage to wrap as an attribute.
   * @param name The name of the attribute.
   */
  public PhantomSupportingNodeAttribute(
    final Model model, final Object mutex, final SlotInfo<T> attr,
    final String name, final PhantomNodeIdentifier pnHelper )
  {
    structLock = mutex;
    helper = pnHelper;
    delegate = new GuardedImmutableNodeAttribute<T>( model, mutex, attr, name );
  }

  /**
   * If <code>node</code> is a phantom node then return the non-proxy node that it 
   * maps to, otherwise return <code>node</code>.
   */
  private IRNode fixNode( final IRNode node )
  {
    return helper.isPhantomNode( node ) ? helper.mapPhantomNode( node ) : node;
  }
  
  @Override
  protected boolean valueExists( final IRNode node )
  {
    synchronized( structLock ) {
      return fixNode( node ).valueExists( delegate );
    }
  }

  @Override
  protected T getSlotValue( final IRNode node )
  throws SlotUndefinedException
  {
    synchronized( structLock ) {
      return fixNode( node ).getSlotValue( delegate );
    }
  }
  
  @Override
  protected void setSlotValue( final IRNode node, final T newValue)
  throws SlotImmutableException
  {
    // don't do anything special here---let the delegate throw the exception
    node.setSlotValue( delegate, newValue );
  }
  
  @Override
  public int size() {
    return delegate.size();
  }
}
