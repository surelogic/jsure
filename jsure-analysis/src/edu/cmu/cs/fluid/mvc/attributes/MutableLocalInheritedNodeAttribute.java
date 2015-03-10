/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attributes/MutableLocalInheritedNodeAttribute.java,v 1.10 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.attributes;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * SlotInfoWrapper for mutable inherited attributes.
 * The original attribute is used until the first time
 * the attribute is set, at which point local storage
 * takes over.  Currently there is no way to "forget" a
 * local value.
 */
public final class MutableLocalInheritedNodeAttribute<T>
extends MutableDelegatingSlotInfo<T>
{
  /** The model the attribute is a part of. */
  protected final Model partOf;

  /** The lock to use to protect that attribute value. */
  protected final Object structLock;
  
  /** The name of the attribute; needed for the attribute changed callback. */
  private final String attr;

  /** The attribute changed callback to use. */
  private final AttributeChangedCallback callback;

  /**
   * Create a new slotinfo.
   */
  public MutableLocalInheritedNodeAttribute(
    final Model model, final Object mutex, final String name, 
    final SlotInfo<T> attribute, final SlotFactory sf,
    final AttributeChangedCallback cb )
  {
    super(attribute, sf);
    partOf = model;
    structLock = mutex;
    attr = name;
    callback = cb;
    /*
     * 
    SlotInfo temp = null;

    try {
      temp = sf.newAttribute(   name + "-local-" + this.hashCode() + "-"
                              + partOf.hashCode(), attribute.getType() );
    } catch( final SlotAlreadyRegisteredException e ) {
      // Shouldn't happen?
      System.err.println( "Got SlotAlreadyRegisteredException!" );
      e.printStackTrace( System.err );
      System.err.println( "Continuing..." );
    }
    */
  }

  @Override
  protected final boolean valueExists( final IRNode node )
  {
    synchronized( structLock ) {
      if( partOf.isAttributable( node, attr ) ) {
        return super.valueExists(node);
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
        return super.getSlotValue(node);
      } else {
        throw new SlotUndefinedException(
            "Node " + node + " is not in model \""
          + partOf.getName() + "\"." );
      }
    }
  }
  
  @Override
  protected final void setSlotValue( final IRNode node, final T newValue )
  throws SlotImmutableException
  {
    synchronized( structLock ) {
      if( partOf.isAttributable( node, attr ) ) {
        super.setSlotValue( node, newValue );
      } else {
        throw new SlotUndefinedException(
            "Node " + node + " is not in model \""
          + partOf.getName() + "\"." );
      }
    }
    callback.attributeChanged( attr, node, newValue );
  }
}
