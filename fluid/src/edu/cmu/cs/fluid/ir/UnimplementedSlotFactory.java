// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/UnimplementedSlotFactory.java,v 1.11 2008/10/27 15:26:44 chance Exp $
package edu.cmu.cs.fluid.ir;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.NotImplemented;

/** A slot factory for which every
 * method throws an NotImplemented exception.
 * This interface is used in MVC when most of the methods are
 * unneeded.
 */
public abstract class UnimplementedSlotFactory implements ExplicitSlotFactory {
  /** Return a (possibly shared) undefined slot of the family. */
  public <T> Slot<T> undefinedSlot() { throw new NotImplemented(); }
  
  /** Return a (possibly shared) predefined slot of the family. */
  public <T> Slot<T> predefinedSlot(T value) { throw new NotImplemented(); }

  /** Return a new sequence of the specific size.
   * Three possibilities exist: <dl>
   * <dt>size &lt; 0<dd> a list is returned initialized to have
   *                     ~size elements (-1 means none).
   * <dt>size   =  0<dd> an empty sequence is returned.
   * <dt>size &gt; 0<dd> a fixed size array is returned.</dl>
   */
  public <T> IRSequence<T> newSequence(int size)  { throw new NotImplemented(); }

  /** Convert an iterator to be "stable" in some sense.
   * Unless versions are involved, it is an NOP.
   */
  public <T> Iteratable<T> newIterator(Iteratable<T> e) { throw new NotImplemented(); }
  
  /** Convert an list iterator to be "stable" in some sense.
   * Unless versions are involved, it is an NOP.
   */
  public <T> ListIteratable<T> newListIterator(ListIteratable<T> e) { throw new NotImplemented(); }
    
  /** Create a new anonymous attribute. */
  public <T> SlotInfo<T> newAttribute() { throw new NotImplemented(); }
  
  public <T> SlotInfo<T> newLabeledAttribute(String l) { throw new NotImplemented(); }

  public <T> SlotInfo<T> newLabeledAttribute(String l, T val) { throw new NotImplemented(); }
  
  /** Create a new named (possibly persistent) attribute. 
   * @throws SlotAlreadyRegisteredException
   * 	if a slot with this name already exists.
   * @precondition nonNull(name)
   */
  public <T> SlotInfo<T> newAttribute(String name, IRType<T> type) 
    throws SlotAlreadyRegisteredException
  { throw new NotImplemented(); }

  /** Create a new anonymous attribute with a default value. */
  public <T> SlotInfo<T> newAttribute(T defaultValue) 
  { throw new NotImplemented(); }
    
  /** Create a new named (possibly persistent) attribute wiith
   * a default value.
   * @throws SlotAlreadyRegisteredException
   * 	if a slot with this name already exists.
   * @precondition nonNull(name)
   */
  public <T> SlotInfo<T> newAttribute(String name, IRType<T> type, T defaultValue)  
    throws SlotAlreadyRegisteredException
  { throw new NotImplemented(); }
    
  
  public AbstractChangeRecord newChangeRecord(String name) throws SlotAlreadyRegisteredException {
    throw new NotImplemented();
  }

  public <T> SlotStorage<?,T> getStorage() {
    throw new NotImplemented();
  }
  
  public void noteChange(IRState state) {
    throw new NotImplemented();
  }
  /** Return the slot factory to be used for "old" slots. */
  public SlotFactory getOldFactory() { throw new NotImplemented(); }
}
