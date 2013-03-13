/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SlotFactory.java,v 1.18 2008/10/27 15:26:44 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import com.surelogic.common.util.*;

/** An interface to a family of slots.
 * This interface can be used to parameterize complex objects.
 * @see AbstractImplicitSlotFactory
 * @see ExplicitSlotFactory
 * @see SimpleSlotFactory
 * @see ConstantSlotFactory
 * @see edu.cmu.cs.fluid.version.VersionedSlotFactory
 */
public interface SlotFactory {
  /** Return a new sequence of the specific size.
   * Three possibilities exist: <dl>
   * <dt>size &lt; 0<dd> a list is returned initialized to have
   *                     ~size elements (-1 means none).
   * <dt>size   =  0<dd> an empty sequence is returned.
   * <dt>size &gt; 0<dd> a fixed size array is returned.</dl>
   */
  public <T> IRSequence<T> newSequence(int size);
  
  /** Convert an enumeration to be "stable" in some sense.
   * Unless versions are involved, it is an NOP.
   */
  public <T> Iteratable<T> newIterator(Iteratable<T> e);
  
  /** Convert an enumeration to be "stable" in some sense.
   * Unless versions are involved, it is an NOP.
   */
  public <T> ListIteratable<T> newListIterator(ListIteratable<T> e);

  /** Create a new anonymous attribute. */
  public <T> SlotInfo<T> newAttribute();
  
  /** Create a new anonymous, but labeled attribute. */
  public <T> SlotInfo<T> newLabeledAttribute(String label);

  /** Create a new anonymous, but labeled attribute. */
  public <T> SlotInfo<T> newLabeledAttribute(String label, T defaultVal);
  
  /** Create a new named (possibly persistent) attribute. 
   * @throws SlotAlreadyRegisteredException
   * 	if a slot with this name already exists.
   * @precondition nonNull(name)
   */
  public <T> SlotInfo<T> newAttribute(String name, IRType<T> type) 
       throws SlotAlreadyRegisteredException;
  /** Create a new anonymous attribute with a default value. */
  public <T> SlotInfo<T> newAttribute(T defaultValue);
  /** Create a new named (possibly persistent) attribute wiith
   * a default value.
   * @throws SlotAlreadyRegisteredException
   * 	if a slot with this name already exists.
   * @precondition nonNull(name)
   */
  public <T> SlotInfo<T> newAttribute(String name, IRType<T> type, T defaultValue)  
       throws SlotAlreadyRegisteredException;

  /**
   * Return a new registered change record.
   * @param name name of record
   * @return newly created change record
   * @throws SlotAlreadyRegisteredException
   */
  public AbstractChangeRecord newChangeRecord(String name) throws SlotAlreadyRegisteredException;
  
  /**
   * The state given has detected that it has changed.  It informs the slot
   * factory, that may or may not do anything.
   * @param state
   */
  public void noteChange(IRState state);
  
  public <T> SlotStorage<?,T> getStorage();
  
  /** Return the slot factory to be used for "old" slots. */
  public SlotFactory getOldFactory();
}
