/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/Slots.java,v 1.14 2007/04/17 18:07:05 chance Exp $ */
package edu.cmu.cs.fluid.ir;

/** Interface to slot storage.
 */
public interface Slots<S,T> {
  /**
   * @return true, if the implementation handles its own synchronization
   */
  public boolean isThreadSafe();
	
  /** Return the slot state for this key (or noSlotState).
   * @param key key (node or slot info) to use to get slot.
   */
  public S getSlot(IRNode key, S defaultSlotState);

  /** Change the association for a key.
   * @param key key (node or slot info) to use to get slot.
   * @param slotState slot state to be stored.
   * @return the former slotState (or defaultSlotState) for this key
   */
  public S setSlot(IRNode key, S slotState, S defaultSlotState);

  /** Return some indication of how many things are stored. */
  public int size();

  //!! need to add enumeration too
  public int cleanup();

  public boolean compact();
  
  /**
   * FIX this shouldn't be called for versioned slots
   */
  public void undefineSlot(IRNode node);

}