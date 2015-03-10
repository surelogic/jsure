/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attributes/LockedComponentSlotWrapper.java,v 1.9 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.attributes;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * A wrapper for ComponentAttributes that protects its value using
 * a given lock object.
 */
public class LockedComponentSlotWrapper<T>
extends ComponentSlotWrapper<T>
{
  /** The lock object to use. */
  private final Object mutex;

  /** Create a new wrapper. */
  public LockedComponentSlotWrapper( final ComponentSlot<T> ca,
				     final Object mtx )
  {
    super( ca );
    mutex = mtx;
  }

  // inherit java doc
  @Override
  public boolean isChanged()
  {
    synchronized( mutex ) { return super.isChanged(); }
  }

  // inherit java doc
  @Override
  public boolean isValid()
  {
    synchronized( mutex ) { return super.isValid(); }
  }

  // inherit java doc
  @Override
  public T getValue()
  {
    synchronized( mutex ) { return super.getValue(); }
  }

  // inherit java doc
  @Override
  public Slot<T> setValue( final T value )
  throws SlotImmutableException
  { 
    synchronized( mutex ) {
      super.setValue( value ); 
      return this;
    }
  }

  @Override
  public Slot<T> readValue( final IRType<T> t, final IRInput in )
  throws java.io.IOException
  {
    synchronized( mutex ) {
      return super.readValue( t, in );
    }
  }

  @Override
  public void writeValue( final IRType<T> t, final IROutput out )
  throws java.io.IOException
  {
    synchronized( mutex ) {
      super.writeValue( t, out );
    }
  }
}
