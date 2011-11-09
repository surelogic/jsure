/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SlotStorage.java,v 1.1 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.io.PrintStream;


/**
 * A ADT that knows how to manage something of type
 * S to store things of type T.
 * @author boyland
 */
public interface SlotStorage<S,T> {
  /**
   * @return true, if the implementation handles its own synchronization
   */
  public boolean isThreadSafe();
	
  public S newSlot();
  public S newSlot(T initialValue);
  public T getSlotValue(S slotState);
  public S setSlotValue(S slotState, T newValue);
  public boolean isValid(S slotState);
  public boolean isChanged(S slotState);
  public void writeSlotValue(S slotState, IRType<T> ty, IROutput out) throws IOException;
  public S readSlotValue(S slotState, IRType<T> ty, IRInput in) throws IOException;
  public void describe(S slotState, PrintStream out);
  public SlotFactory getSlotFactory();
}
