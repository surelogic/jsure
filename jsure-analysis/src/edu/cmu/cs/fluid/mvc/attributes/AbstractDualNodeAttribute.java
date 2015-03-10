/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attributes/AbstractDualNodeAttribute.java,v 1.9 2006/03/29 18:30:56 chance Exp $ */
package edu.cmu.cs.fluid.mvc.attributes;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * A component slot wrapper that can switch between two different
 * component slots of the same IRType.  The switching is controlled
 * by the abstract method {@link #which}.  Intended for use by
 * attribute managers.
 * 
 * @author Aaron Greenhouse
 */
public abstract class AbstractDualNodeAttribute<T>
extends SlotInfo<T>
{
  /** The model the attribute is a part of. */
  protected final Model partOf;

  /** The lock to use to protect that attribute value. */
  protected final Object structLock;
    
  /** The first attribute. */
  protected final SlotInfo<T> si1;

  /** The second attribute. */
  protected final SlotInfo<T> si2;
  
  /** The name of the attribute; needed for the attribute changed callback. */
  private final String attr;

  /** The attribute changed callback to use. */
  private final AttributeChangedCallback callback;

  /**
   * Create a new multiplexing slotinfo.  The two SlotInfos that it 
   * switches between must have the same IRType.
   * @exception IllegalArgumentException Thrown if the two SlotInfos do not have
   * the same type.
   */
  public AbstractDualNodeAttribute(
    final Model model, final Object mutex, final String name, 
    final SlotInfo<T> s1, final SlotInfo<T> s2, final AttributeChangedCallback cb )
  {
    // Sanity check
    if( !s1.type().equals( s2.type() ) ) {
      throw new IllegalArgumentException( "The types of the two SlotInfos don't match" );
    }
    partOf = model;
    structLock = mutex;
    attr = name;
    callback = cb;
    si1 = s1;
    si2 = s2;
  }

  /**
   * Determine which SlotInfo should be used.  The node being operated on
   * is provided and is allowed to affect which SlotInfo is used.
   * @return <code>true</code> if the first SlotInfo should be used,
   *         <code>false</code> if the second SlotInfo should be used.
   */
  public abstract boolean which( final IRNode node );
  
  /**
   * Return the SlotInfo to be used.  The node being operated on
   * is provided and is allowed to affect which SlotInfo is used.
   */
  private SlotInfo<T> whichSlotInfo( final IRNode node ) 
  {
    return which( node ) ? si1 : si2;
  }
    
  @Override
  public final IRType<T> type() {
    return si1.type();
  }

  @Override
  protected final boolean valueExists( final IRNode node )
  {
    synchronized( structLock ) {
      if( partOf.isAttributable( node, attr ) ) {
        return node.valueExists( whichSlotInfo( node ) );
      } else {
        return false;
      }
    }
  }

  @Override
  protected final T getSlotValue( final IRNode node )
  throws SlotUndefinedException
  {
    synchronized( structLock ) {
      if( partOf.isAttributable( node, attr ) ) {
        return node.getSlotValue( whichSlotInfo( node ) );
      } else {
        throw new SlotUndefinedException(
            "Node " + node + " is not in model \""
          + partOf.getName() + "\"." );
      }
    }
  }
  
  /**
   * Left non-final because we need to be able to pre and post processing.
   * Delegates actual work to {@link #doSetSlotValue}.
   */
  @Override
  protected void setSlotValue( final IRNode node, final T newValue)
  throws SlotImmutableException
  {
    doSetSlotValue( node, newValue );
  }

  /**
   * Performs the actual work for {@link #setSlotValue}.  Moved to a distinct
   * method so that setSlotValue may be re-implemented to perform pre- and post-
   * processing.  Calls the callback (when no exception is thrown).
   */
  protected final void doSetSlotValue( final IRNode node, final T newValue )
  throws SlotImmutableException
  {
    synchronized( structLock ) {
      if( partOf.isAttributable( node, attr ) ) {
        node.setSlotValue( whichSlotInfo( node ), newValue );
      } else {
	throw new SlotImmutableException(
            "Node " + node + " is not in model \""
          + partOf.getName() + "\"." );
      }
    }
    callback.attributeChanged( attr, node, newValue );
  }
  
  @Override
  public int size() {
    return si1.size() + si2.size();
  }
}
