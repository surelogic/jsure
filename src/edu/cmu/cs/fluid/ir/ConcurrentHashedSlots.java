package edu.cmu.cs.fluid.ir;

import java.util.concurrent.*;

/** Storage of slots in a ConcurrentHashMap
 * @typeparam Key key (either node or slot info) for slot
 */
public class ConcurrentHashedSlots<S,T> extends ConcurrentHashMap<IRNode,S> implements Slots<S,T> {
  private static final long serialVersionUID = 1L;
  
  private static final IRNode nullNode = new MarkedIRNode("Null");
  
  @SuppressWarnings("unchecked")
  private S nullSlot() {
	  return (S) nullNode;
  }
  
  public boolean isThreadSafe() {
	  return true;
  }
  
  public S getSlot(IRNode key, S noSlotState) {
    /*
    if (containsKey(key))
      return get(key);
    else
      return noSlotState;
      */
	if (key == null) {
		key = nullNode;
	}
    S o = get(key);    
    if (o == null) {
    	return noSlotState;
    }
    else if (o == nullSlot()) {
    	return null;
    } 
    else {
    	// This is definitely mapped
        return o;
    }
  }
  
  public S setSlot(IRNode key, S slot, S noSlotState) {
	if (key == null) {
		key = nullNode;
	}
	if (slot == null) {
		slot = nullSlot();
	}
    return put(key, slot);
  }
  
  /**
   * FIX this shouldn't be called for versioned slots
   */
  public void undefineSlot(IRNode node) {
	if (node == null) {
		node = nullNode;
	}
    remove(node);
  }

  @Override
  public int cleanup() {
	  // TODO Auto-generated method stub
	  return 0;
  }

  @Override
  public boolean compact() {
	  // TODO Auto-generated method stub
	  return false;
  }
}
