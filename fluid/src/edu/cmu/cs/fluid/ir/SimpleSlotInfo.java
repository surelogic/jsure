/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SimpleSlotInfo.java,v 1.31 2008/10/27 15:26:44 chance Exp $ */
package edu.cmu.cs.fluid.ir;

/** An attribute with values stored directly in a hashtable. 
 * @deprecated use SimpleSlotFactory.makeSlotInfo
 */
@SuppressWarnings("unused")
@Deprecated
public class SimpleSlotInfo<T> extends InfoStoredSlotInfo<T,T> implements PersistentSlotInfo<T> {
  private static <T> Slots<T,T> getSlots() {
    return new HashedSlots<T,T>();
  }
  
  private SimpleSlotInfo() {
    super(SimpleSlotFactory.prototype.<T>getStorage(), null, SimpleSlotInfo.<T>getSlots());
  }

  private SimpleSlotInfo(T val) {
    super(SimpleSlotFactory.prototype.<T>getStorage(), null, val,SimpleSlotInfo.<T>getSlots());
  }

  private SimpleSlotInfo(String name, IRType<T> type) throws SlotAlreadyRegisteredException {
    super(name,type,SimpleSlotFactory.prototype.<T>getStorage(),SimpleSlotInfo.<T>getSlots());
  }

  private SimpleSlotInfo(String name, IRType<T> type, T val) throws SlotAlreadyRegisteredException {
    super(name,type,SimpleSlotFactory.prototype.<T>getStorage(),val,SimpleSlotInfo.<T>getSlots());
  }

}
