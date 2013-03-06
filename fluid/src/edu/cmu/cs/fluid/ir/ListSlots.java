package edu.cmu.cs.fluid.ir;

import edu.cmu.cs.fluid.util.AssocList;

/** Storage of slots in an association list.
 * @typeparam Key key (either node or slot info) for slot
 */
public class ListSlots<S,T> extends AssocList<IRNode,S> implements Slots<S,T> {
  public ListSlots() {
    super();
  }
  
  @Override
  public boolean isThreadSafe() {
	  return false;
  }
  
  @Override
  public S getSlot(IRNode key, S def) {
    return get(key);
  }
  @Override
  public S setSlot(IRNode key, S slot, S def) {
    return put(key, slot);
  }
  @Override
  public int cleanup() {
    // TODO ignored
    return 0;
  }
  
  @Override
  public boolean compact() {
    // Nothing to reallocate
    return false;
  }
  
  /**
   * FIX this shouldn't be called for versioned slots
   */
  @Override
  public void undefineSlot(IRNode node) {
    remove(node);
  }
}