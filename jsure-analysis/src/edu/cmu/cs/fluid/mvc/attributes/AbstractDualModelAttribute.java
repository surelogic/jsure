/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attributes/AbstractDualModelAttribute.java,v 1.8 2006/03/29 18:30:56 chance Exp $ */
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
public abstract class AbstractDualModelAttribute<T> extends DefaultDescribe
implements ComponentSlot<T>
{
  /** The lock to use to protect that attribute value. */
  protected final Object structLock;
    
  /** The first attribute. */
  protected final ComponentSlot<T> slot1;

  /** The second attribute. */
  protected final ComponentSlot<T> slot2;
  
  /** The name of the attribute; needed for the attribute changed callback. */
  private final String attr;

  /** The attribute changed callback to use. */
  private final AttributeChangedCallback callback;

  /**
   * Create a new multiplexing component slot.  The two slots that it 
   * switches between must have the same IRType.
   * @exception IllegalArgumentException Thrown if the two slots do not have
   * the same type.
   */
  public AbstractDualModelAttribute(
    final Object mutex, final String name, 
    final ComponentSlot<T> s1, final ComponentSlot<T> s2,
    final AttributeChangedCallback cb )
  {
    // Sanity check
    if( !s1.getType().equals( s2.getType() ) ) {
      throw new IllegalArgumentException( "The types of the two slots don't match" );
    }
    structLock = mutex;
    attr = name;
    callback = cb;
    slot1 = s1;
    slot2 = s2;
  }

  /**
   * Determine which slot should be used.
   * @return <code>true</code> if the first slot should be used,
   *         <code>false</code> if the second slot should be used.
   */
  protected abstract boolean which();
  
  /**
   * Return the slot to be used.
   */
  private ComponentSlot<T> whichSlot() 
  {
    return which() ? slot1 : slot2;
  }
    
  @Override
  public final IRType<T> getType() { return slot1.getType(); }
  
  @Override
  public final boolean isChanged()
  {
     synchronized( structLock ) {
       return whichSlot().isChanged();
     }
  }
  
  @Override
  public final boolean isValid() 
  {
    synchronized( structLock ) {
      return whichSlot().isValid();
    }
  }
    
  @Override
  public final T getValue() 
  {
    synchronized( structLock ) {
      return whichSlot().getValue();
    }
  }

  /**
   * Left non-final because we need to be able to change the return value
   * if we are wrapped.  Delegates actual work to {@link #doSetValue}.
   */
  @Override
  public Slot<T> setValue( final T value )
  throws SlotImmutableException
  {
    doSetValue( value );
    return this;
  }

  /**
   * Performs the actual work for {@link #setValue}.  Moved to a distinct
   * method so that setValue may be re-implemented to perform pre- and post-
   * processing.  Calls the callback (when no exception is thrown).
   */
  protected final void doSetValue( final T value )
  throws SlotImmutableException
  {
    synchronized( structLock ) {
      whichSlot().setValue( value );
    }
    callback.attributeChanged( attr, null, value );
  }
  
  /**
   * This is probably wrong!
   */
  @Override
  public final Slot<T> readValue( final IRType<T> t, final IRInput in )
  throws java.io.IOException
  {
    return whichSlot().readValue( t, in );
  }

  /**
   * This is probably wrong!
   */
  @Override
  public final void writeValue( final IRType<T> t, final IROutput out )
  throws java.io.IOException
  {
    whichSlot().writeValue( t, out );
  }
}
