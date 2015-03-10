/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/ConstantSlotFactory.java,v 1.17 2007/07/10 22:16:31 aarong Exp $ */
package edu.cmu.cs.fluid.ir;


/** The family of constant (immutable) slots. */
public class ConstantSlotFactory extends AbstractImplicitSlotFactory {
  protected ConstantSlotFactory() {}
  public static final ConstantSlotFactory prototype = 
       new ConstantSlotFactory();
  static {
    IRPersistent.registerSlotFactory(prototype,'C');
  }

  private static final SlotStorage defaultStorage = new ConstantImplicitSlotStorage();
  
  @SuppressWarnings("unchecked")
  @Override
  public <T> SlotStorage<T, T> getStorage() {
    return defaultStorage;
  }

  @Override
  public SlotFactory getOldFactory() {
    return ConstantSlotFactory.prototype;
  }
  
  @Override
  public void noteChange(IRState state) {
    // DO nothing (constant information has no changes)
  }
  public static void ensureLoaded() {}
}

class ConstantImplicitSlotStorage<T> extends ImplicitSlotStorage<T> {

  public ConstantImplicitSlotStorage() {
    super(ConstantSlotFactory.prototype);
  }

  @Override
  public T setSlotValue(T slotState, T newValue) {
    if (!isValid(slotState)) {
      return super.setSlotValue(slotState,newValue);
    } else throw new SlotImmutableException("constant implicit slot is immutable");
  }
  
}

class OldConstantSlotFactory extends ConstantSlotFactory {
  private static final SlotStorage defaultStorage = new OldConstantImplicitSlotStorage();
  
  @SuppressWarnings("unchecked")
  @Override
  public <T> SlotStorage<T, T> getStorage() {
    return defaultStorage;
  }

}

class OldConstantImplicitSlotStorage<T> extends ImplicitSlotStorage<T> {

  public OldConstantImplicitSlotStorage() {
    super(ConstantSlotFactory.prototype);
  }

  @Override
  public T setSlotValue(T slotState, T newValue) {
    throw new SlotImmutableException("old constant implicit slot is immutable");
  }
  
}

