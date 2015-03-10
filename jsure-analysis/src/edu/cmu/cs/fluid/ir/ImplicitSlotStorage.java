/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/ImplicitSlotStorage.java,v 1.3 2008/06/30 15:22:47 chance Exp $*/
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.io.PrintStream;


/**
 * Slot storage without any intermediate objects
 * @author boyland
 */
public class ImplicitSlotStorage<T> implements SlotStorage<T, T> {
  /*
   * The following definition appears completely untypesafe,
   * but actually works because we never return it from anything 
   * that is actually expecting something of type T.
   * null is not safe here because "null" is a legal value for most slots.
   */
  @SuppressWarnings("unchecked")
  private final T undefinedValue;
  private final SlotFactory factory;
  
  public ImplicitSlotStorage(SlotFactory sf) {
    factory = sf;
    undefinedValue = (T) new Object();
  }
  
  public ImplicitSlotStorage(SlotFactory sf, T undefVal) {
    factory = sf;
    undefinedValue = undefVal;
  }
  
  @Override
  public boolean isThreadSafe() {
	// Ok because there's no state to worry about
	return true;
  }
  
  @Override
  public SlotFactory getSlotFactory() {
    return factory;
  }
  
  @Override
  public T newSlot() {
    return undefinedValue;
  }
  @Override
  public T newSlot(T initialValue) {
    return initialValue;
  }
  @Override
  public T getSlotValue(T slotState) {
    if (slotState != undefinedValue) return slotState;
    throw new SlotUndefinedException("undefined implicit slot");
  }
  @Override
  public T setSlotValue(T slotState, T newValue) {
    return newValue;
  }
  @Override
  public boolean isValid(T slotState) {
    return slotState != undefinedValue;
  }
  @Override
  public boolean isChanged(T slotState) {
    return false; // we do not check without implicit slots
  }
  @Override
  public void writeSlotValue(T slotState, IRType<T> ty, IROutput out) throws IOException {
    ty.writeValue(slotState,out);
  }
  @Override
  public T readSlotValue(T slotState, IRType<T> ty, IRInput in) throws IOException {
    return ty.readValue(in);
  }

  @Override
  public void describe(T slotState, PrintStream out) {
    if (slotState == undefinedValue) {
      out.print("<undefined>");
    } else {
      out.print(slotState);
    }
  }
  
  protected T getUndefinedValue() {
	  return undefinedValue;
  }
}
