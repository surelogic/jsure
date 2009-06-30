/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attributes/GuardedImmutableModelAttribute.java,v 1.8 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.attributes;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * ComponentSlot wrapper intended for use by AttributeManagers that
 * insures that the component slot is made part of the Model's monitor
 * (this is accomplished by subclassing {@link LockedComponentSlotWrapper}),
 * enforces immutability (when required), and calls the callback.
 *
 * @author Aaron Greenhouse
 */
public final class GuardedImmutableModelAttribute<T>
extends LockedComponentSlotWrapper<T>
{
  /**
   * The model the attribute belongs to; needed to get the model's name
   * for the exception.  Cannot simply hold a reference to the name because
   * these object might be created during the initialization of ModelCore
   * itself, in which case the name won't yet be directly available to the
   * caller of this class's constructor.
   */
  private final Model partOf;
   
  /**
   * The attribute name; needed to get the model's name
   * for the exception.  Cannot simply hold a reference to the name because
   * these object might be created during the initialization of ModelCore
   * itself, in which case the name won't yet be directly available to the
   * caller of this class's constructor.
   */
  private final String attr;
   
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
  public GuardedImmutableModelAttribute(
    final Model model, final Object mutex, final ComponentSlot<T> ca,
    final String attrName )
  {
    super( ca, mutex );
    partOf = model;
    attr = attrName;
  }
    
  /**
   * Overridden to enforce immutability.
   */
  @Override
  public Slot<T> setValue( final T value )
  {
    throw new SlotImmutableException( 
                  "Component-level attribute \"" + attr + "\" of model \""
                + partOf.getName() + "\" is immutable." );
  }
}
