package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.ir.*;

/**
 * A simple wrapper for ComponentAttributes.
 */
public class ComponentSlotWrapper<T> extends DefaultDescribe
implements ComponentSlot<T>
{
  /** The wrapped attribute. */
  protected final ComponentSlot<T> wrapped;

  /** Create a new wrapper. */
  public ComponentSlotWrapper( final ComponentSlot<T> ca )
  {
    wrapped = ca;
  }

  @Override
  public IRType<T> getType() { return wrapped.getType(); }
  @Override
  public boolean isChanged() { return wrapped.isChanged(); }
  @Override
  public boolean isValid() { return wrapped.isValid(); }
  @Override
  public T getValue() { return wrapped.getValue(); }

  @Override
  public Slot<T> setValue( final T value )
  throws SlotImmutableException
  {
    wrapped.setValue( value );
    return this;
  }

  @Override
  public Slot<T> readValue( final IRType<T> t, final IRInput in )
  throws java.io.IOException
  {
    return wrapped.readValue( t, in );
  }

  @Override
  public void writeValue( final IRType<T> t, final IROutput out )
  throws java.io.IOException
  {
    wrapped.writeValue( t, out );
  }
}
