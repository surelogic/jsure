/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/ComponentSlot.java,v 1.10 2008/06/26 17:55:54 chance Exp $ */
package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.ir.IRType;
import edu.cmu.cs.fluid.ir.Slot;

/**
 * Objects that store the values for component-level attributes
 * must conform to this interface.  
 *
 * @author Aaron Greenhouse
 */
public interface ComponentSlot<T>
extends Slot<T>
{
  /**
   * Get the type of the value stored by the attribute.
   * The type of the value must remain constant.
   */
  public IRType<T> getType();

  /**
   * Set the value stored by the attribute.
   * Unlike {@link edu.cmu.cs.fluid.ir.Slot#setValue}, it must always return
   * the a reference to itself.
   */
  //public Slot<T> setValue( T value );



  /**
   * Interface for factory objects that create {@link ComponentSlot}s.
   * Each factory object creates a related "family" of ComponentSlots.
   *
   * @author Aaron Greenhouse
   */
  public static interface Factory
  {
    /**
     * Return a (possibly shared) undefined ComponentSlot of the family.
     * @param type The type of the value stored in the ComponentSlot.
     */
    public <T> ComponentSlot<T> undefinedSlot( IRType<T> type );

    /**
     * Return a (possibly shared) predefined ComponentSlot of the family.
     * @param type The type of the value stored in the ComponentSlot.
     * @param value The value with which to initialize the ComponentSlot.
     * @exception IllegalArgumentException Thrown if provided value is not
     * of the appropriate IRType.
     */
    public <T> ComponentSlot<T> predefinedSlot( IRType<T> type, T value );
  }
}
