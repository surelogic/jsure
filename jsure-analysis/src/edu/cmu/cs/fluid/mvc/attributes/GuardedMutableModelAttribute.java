/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attributes/GuardedMutableModelAttribute.java,v 1.8 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.attributes;

import edu.cmu.cs.fluid.ir.Slot;
import edu.cmu.cs.fluid.mvc.*;

/**
 * ComponentSlot wrapper intended for use by AttributeManagers
 * that insures that the component slot is made part of the Model's monitor
 * (this is accomplished by subclassing {@link LockedComponentSlotWrapper})
 * and that calls the attribute changed callback.
 *
 * @author Aaron Greenhouse
 */
public final class GuardedMutableModelAttribute<T>
extends LockedComponentSlotWrapper<T>
{
  /** The name of the attribute; needed for the callback. */
  private final String attr;
  
  /** The attribute changed callback to invoke when the value is changed. */
  private final AttributeChangedCallback callback;
    
  /**
   * <code>ca</code> should not be a ComponentSlot obtained
   * from viewed model, etc.  It should be a freshly generated
   * object, created by the model associated with this
   * inner class's AttributeManager.  In particular, it is assumed
   * that if <code>cb</code> is not a null callback, then
   * setting the value of <code>ca</code> <em>will not</em>
   * cause the model to send an event or otherwise visibly
   * react to the change in value.
   */
  public GuardedMutableModelAttribute(
    final Object mutex, final ComponentSlot<T> ca, final String name,
    final AttributeChangedCallback cb )
  {
    super( ca, mutex );
    attr = name;
    callback = cb;
  }
    
  /** Overridden to call the callback */
  @Override
  public Slot<T> setValue( final T value )
  {
    super.setValue( value );
    callback.attributeChanged( attr, null, value );
    return this;
  }
}
