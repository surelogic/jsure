/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/ExplicitSlotStorage.java,v 1.1 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.io.PrintStream;


/**
 * TODO Fill in purpose.
 * @author boyland
 */
public class ExplicitSlotStorage<T> implements SlotStorage<Slot<T>, T> {
  private final ExplicitSlotFactory factory;
  ExplicitSlotStorage(ExplicitSlotFactory sf) {
    factory = sf;
  }
  
  public SlotFactory getSlotFactory() {
    return factory;
  }

  public boolean isThreadSafe() {
	return false;
  }
  
  public Slot<T> newSlot() {
    return factory.undefinedSlot();
  }
  public Slot<T> newSlot(T initialValue) {
    return factory.predefinedSlot(initialValue);
  }
  public T getSlotValue(Slot<T> slotState) {
    return slotState.getValue();
  }
  public Slot<T> setSlotValue(Slot<T> slotState, T newValue) {
    return slotState.setValue(newValue);
  }
  public boolean isValid(Slot<T> slotState) {
    return slotState.isValid();
  }
  public boolean isChanged(Slot<T> slotState) {
    return slotState.isChanged();
  }
  public void writeSlotValue(Slot<T> slotState, IRType<T> ty, IROutput out) throws IOException {
    slotState.writeValue(ty,out);
  }
  public Slot<T> readSlotValue(Slot<T> slotState, IRType<T> ty, IRInput in) throws IOException {
    return slotState.readValue(ty,in);
  }
  public void describe(Slot<T> slotState, PrintStream out) {
    slotState.describe(out);
  }

}
