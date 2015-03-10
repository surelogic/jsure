/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attributes/MutableLocalInheritedModelAttribute.java,v 1.8 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.attributes;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * Wrapper for a mutable derived component attribute.
 * The original attribute is used until the first time
 * the attribute is set, at which point local storage
 * takes over.  Currently there is no way to "forget" a
 * local value.
 */
public final class MutableLocalInheritedModelAttribute<T>
extends AbstractDualModelAttribute<T>
{
  /** Has the value been mutated yet? */
  private boolean notMutated;
   

  
  /**
   * Creates a new instance.
   *
   * @param mutex The mutex used to protect the attribute's value.
   * @param name The name of the attribute; must be interned.
   * @param ca The attribute storage for the inherited attribute.
   * @param sf The SlotFactory to use to create the new local storage.
   * @param cb The callback to use for the attribute.  This callback 
   * is only called when the local storage is changed.
   */
  public MutableLocalInheritedModelAttribute(
    final Object mutex, final String name, final ComponentSlot<T> ca,
    final ComponentSlot.Factory sf, final AttributeChangedCallback cb )
  {
    super( mutex, name, ca, sf.undefinedSlot( ca.getType() ), cb );
    notMutated = true;
  }
  
  /**
   * Use the first (inherited) component slot until we have been mutated,
   * at which point we use the local slot forever after.
   * @return <code>true</code> if the value has not been set yet.
   */
  @Override
  protected final boolean which() { return notMutated; }

  /**
   * Overridden to toggle the mutation flag <em>before</em> the operation
   * is performed so that the "local" slot (the second one) will be used.
   */
  @Override
  public final Slot<T> setValue( final T value )
  throws SlotImmutableException
  {
    /* If called concurrently this should be okay because doSetValue is
     * synchronized, which will cause the write to propogate between
     * thread memories.  Plus, each thread has set the value to false
     * anyway.
     */
    notMutated = false;
    doSetValue( value );
    return this;
  }
}
