package edu.cmu.cs.fluid.mvc;

import java.io.PrintStream;

import edu.cmu.cs.fluid.ir.*;

/**
 * Simple storage for a component-level attribute that delegates
 * to a {@link edu.cmu.cs.fluid.ir.Slot}.
 */
public class SimpleComponentSlot<T>
  implements ComponentSlot<T>
{
  //=================================================================
  //== Fields
  //=================================================================

  /** The type of the values stored by the attribute. */
  private final IRType<T> type;

  /**
   * The slot used to store the value.  Cannot be <code>final</code>
   * because setting a slot returns a new one.
   */
  private Slot<T> slot;

  

  //=================================================================
  //== Constructors
  //=================================================================

  /**
   * Create new component-level attribute storage.
   * @param t The type of the value stored by the attribute.
   * @param s The initial slot used to store the value of the attribute.
   */
  protected SimpleComponentSlot( final IRType<T> t, final Slot<T> s )
  {
    type = t;
    slot = s;
  }

  /**
   * Create a new undefined component-level attribute.
   * @param t The type of the value stored by the attribute.
   * @param sf The SlotFactory to use to create the initial slot.
   */
  @SuppressWarnings("unchecked")
  public SimpleComponentSlot( final IRType<T> t, final ExplicitSlotFactory sf )
  {
    this( t, (Slot<T>) sf.undefinedSlot() );
  }

  /**
   * Create a new predefined component-level attribute.
   * @param t The type of the value stroed by the attribute.
   * @param sf The SlotFactory to use to create the initial slot.
   * @param v The initial value of the attribute.
   */
  public SimpleComponentSlot( final IRType<T> t, final ExplicitSlotFactory sf,
				final T v )
  {
    this( t, sf.predefinedSlot( v ) );
  }



  //=================================================================
  //== Methods
  //=================================================================

  // inherit java doc
  @Override
  public IRType<T> getType() { return type; }

  // inherit java doc
  @Override
  public boolean isChanged() { return slot.isChanged(); }

  // inherit java doc
  @Override
  public boolean isValid() { return slot.isValid(); }

  // inherit java doc
  @Override
  public T getValue() { return slot.getValue(); }

  // inherit java doc
  @Override
  public Slot<T> setValue( final T value )
  throws SlotImmutableException
  {
    final Slot<T> newSlot = slot.setValue( value );
    slot = newSlot;
    return this;
  }

  // inherit java doc
  @Override
  public Slot<T> readValue( final IRType<T> t, final IRInput in )
  throws java.io.IOException
  {
    // Do something about type?
    return slot.readValue( t, in );
  }

  // inherit java doc
  @Override
  public void writeValue( final IRType<T> t, final IROutput out )
  throws java.io.IOException
  {
    // Do something about type?
    slot.writeValue( t, out );
  }

  /** Describe the wrapped slot. */
  @Override
  public void describe(PrintStream out) {
    slot.describe(out);
  }
}
