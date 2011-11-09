/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attributes/AttrSwitchedDualNodeAttribute.java,v 1.8 2006/03/29 18:30:56 chance Exp $ */
package edu.cmu.cs.fluid.mvc.attributes;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * A component slot wrapper that can switch between two different
 * component slots of the same IRType.  The switching is controlled
 * by a separate boolean-valued node attribute.  Intended for use by
 * attribute managers.
 * 
 * @author Aaron Greenhouse
 */
public final class AttrSwitchedDualNodeAttribute<T>
extends AbstractDualNodeAttribute<T>
{
  /** The attribute that determines which value to use. */
  private final SlotInfo<Boolean> switchAttr;
  
  /**
   * Create a new attribute-switched multiplexing slotinfo.
   * The two SlotInfos that it switches between must have the same IRType.
   * @param model The model the attribute is a part of.
   * @param mutex The lock protected the model that attribute belongs to.
   * @param name The name of the attribute.
   * @param s1 The first attribute.
   * @param s2 The second attribute.
   * @param switcher The attribute that determines which value to use.
   * @param cb The attribute changed callback to use.
   * @exception IllegalArgumentException Thrown if the two SlotInfos do not have
   * the same type or if the switcher attribute is not boolean-valued.
   */
  public AttrSwitchedDualNodeAttribute(
    final Model model, final Object mutex, final String name, 
    final SlotInfo<T> s1, final SlotInfo<T> s2, final SlotInfo<Boolean> switcher,
    final AttributeChangedCallback cb )
  {
    super( model, mutex, name, s1, s2, cb );
    // Sanity check
    if( !switcher.type().equals( IRBooleanType.prototype ) ) {
      throw new IllegalArgumentException(
                  "The switcher attribute is not boolean-valued" );
    }
    switchAttr = switcher;
  }

  /**
   * Determine which SlotInfo should be used.  The value is based on a 
   * separate switcher attribute.
   * @return <code>true</code> if the first SlotInfo should be used,
   *         <code>false</code> if the second SlotInfo should be used.
   */
  @Override
  public final boolean which( final IRNode node )
  {
    return (node.getSlotValue( switchAttr )).booleanValue();
  }
}
