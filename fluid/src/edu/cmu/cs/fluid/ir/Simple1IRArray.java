package edu.cmu.cs.fluid.ir;

/**
 * Specialized from SimpleIRArray be of length 1.
 * Drastically refactored by John Boyland.
 * @author chance
 *
 */
public class Simple1IRArray<T> extends IR1Array<T,T> {
  @Override
  public SlotStorage<T, T> getSlotStorage() {
    return SimpleSlotFactory.prototype.getStorage();
  }
}
