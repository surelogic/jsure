package edu.cmu.cs.fluid.ir;

import com.surelogic.Borrowed;

/** Variable size sequences with locations that stay valid under reshaping
 * Specialized from IRList to save space for SimpleSlots
 */
public class SimpleIRList<T> extends IRList<Integer,T,IRLocation,T> {
  private static final SlotStorage<Integer,Integer> intStorage = 
    new ImplicitSlotStorage<Integer>(SimpleSlotFactory.prototype);
  private static final SlotStorage<IRLocation,IRLocation> locStorage =
    new ImplicitSlotStorage<IRLocation>(SimpleSlotFactory.prototype);

  public SimpleIRList(int n) {
    super(n);
  }
  
  @Override
  protected SlotStorage<Integer, Integer> getIntSlotStorage() {
    return intStorage;
  }
  
  @Override
  @Borrowed("this")
  protected SlotStorage<T, T> getSlotStorage() {
    return SimpleSlotFactory.prototype.<T>getStorage();
  }
  
  @Override
  protected SlotStorage<IRLocation, IRLocation> getElemStorage() {
    return locStorage;
  }
  
  @Override
  protected SlotFactory getSlotFactory() {
    return SimpleSlotFactory.prototype;
  }
  
}