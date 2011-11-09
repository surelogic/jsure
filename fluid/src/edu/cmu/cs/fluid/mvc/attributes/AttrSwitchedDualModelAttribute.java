/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attributes/AttrSwitchedDualModelAttribute.java,v 1.8 2006/03/29 18:30:56 chance Exp $ */
package edu.cmu.cs.fluid.mvc.attributes;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * A component slot wrapper that can switch between two different
 * component slots of the same IRType.  The switching is controlled
 * by the value of a model-level attribute.
 * 
 * @author Aaron Greenhouse
 */
public final class AttrSwitchedDualModelAttribute<T>
extends AbstractDualModelAttribute<T>
{
  /** The attribute whose value controls which value the attribute presents. */
  private final ComponentSlot<Boolean> switchAttr;
  
  /**
   * Create a new attribute-switched multiplexing component slot.
   * The two slots that it switches between must have the same IRType.
   * @param mutex The mutex protecting the model the attribute is a part of.
   * @param name The name of the attribute.
   * @param s1 The first attribute.
   * @param s2 The second attribute.
   * @param switcher The attribute that controls whether the first or
   * second attribute is used.
   * @param cb The attribute changed callback to use.
   * @exception IllegalArgumentException Thrown if the two slots do not have
   * the same type, or if the swicher is not boolean-valued.
   */
  public AttrSwitchedDualModelAttribute(
    final Object mutex, final String name, 
    final ComponentSlot<T> s1, final ComponentSlot<T> s2,
    final ComponentSlot<Boolean> switcher, final AttributeChangedCallback cb )
  {
    super( mutex, name, s1, s2, cb );
    // Sanity check
    if( !switcher.getType().equals( IRBooleanType.prototype ) ) {
      throw new IllegalArgumentException(
                  "The switcher attribute is not boolean-valued." );
    }
    switchAttr = switcher;
  }

  /**
   * Determine which slot should be used, based on the value of the
   * switcher attribute.
   * @return <code>true</code> if the first slot should be used,
   *         <code>false</code> if the second slot should be used.
   */
  @Override
  protected final boolean which()
  {
    return (switchAttr.getValue()).booleanValue();
  }
}
