/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRAbstractParameterizedState.java,v 1.5 2007/07/05 18:15:15 aarong Exp $
 */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.io.PrintStream;


/**
 * An abstract state class which is parameterized by a slot factory.
 * For space reasons, the slot factory is obtained by a method, rather than being stored.
 * This class allows us to be uniform despite the different between a stored slot
 * and an implicit slot.
 * @author boyland
 */
public abstract class IRAbstractParameterizedState<S,T> extends IRAbstractState {
  
  /**
   * Get the (unchanging) slot storage for this state.
   * @return the slot storage for this state.
   */
  public abstract SlotStorage<S,T> getSlotStorage();
  
  @Override
  public SlotFactory getSlotFactory() {
    return getSlotStorage().getSlotFactory();
  }
  
  public T getValue(S slotState) {
    return getSlotStorage().getSlotValue(slotState);
  }
  public S setValue(S slotState, T newValue) {
    return getSlotStorage().setSlotValue(slotState,newValue);
  }
  public boolean isValid(S slotState) {
    return getSlotStorage().isValid(slotState);
  }
  public boolean isChanged(S slotState) {
    return getSlotStorage().isChanged(slotState);
  }
  public void writeValue(S slotState, IRType<T> ty, IROutput out) throws IOException {
    getSlotStorage().writeSlotValue(slotState,ty,out);
  }
  public S readValue(S slotState, IRType<T> ty, IRInput in) throws IOException {
    return getSlotStorage().readSlotValue(slotState,ty,in);
  }
  
  public void describe(S slotState, PrintStream out) {
    getSlotStorage().describe(slotState,out);
  }
}
