package edu.cmu.cs.fluid.ir;

/** Storage of slots in a hashtable
 * @typeparam Key key (either node or slot info) for slot
 */
public class HashedSlots<S,T> extends IRNodeHashedMap<S> implements Slots<S,T> {
  public HashedSlots() {
    super(2);
  }
  
  @Override
  public boolean isThreadSafe() {
	  return false;
  }
  
  @Override
  public S getSlot(IRNode key, S noSlotState) {
    /*
    if (containsKey(key))
      return get(key);
    else
      return noSlotState;
      */
    S o = get(key);
    if (o != null) {
      // This is definitely mapped
      return o;
    }
    // null, so need to see if it really contains the key
    return containsKey(key) ? null : noSlotState;
  }
  
  @Override
  public S setSlot(IRNode key, S slot, S noSlotState) {
    return put(key, slot);
  }
  
  /**
   * FIX this shouldn't be called for versioned slots
   */
  @Override
  public void undefineSlot(IRNode node) {
    remove(node);
  }
}
