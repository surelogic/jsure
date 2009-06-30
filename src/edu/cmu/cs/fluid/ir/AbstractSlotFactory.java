/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/AbstractSlotFactory.java,v 1.17 2008/10/27 15:26:44 chance Exp $
 */
package edu.cmu.cs.fluid.ir;

import edu.cmu.cs.fluid.util.*;


/**
 * An abstract slot factory where most of the methods are given reasonable
 * implementations.  The implementing class need only define
 * {@link #undefinedSlot()} and {@link #predefinedSlot(Object)}.
 * @author boyland
 */
public abstract class AbstractSlotFactory implements SlotFactory {
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.SlotFactory#getOldFactory()
   */
  public SlotFactory getOldFactory() {
    return this;
  }

  protected static <S,T> HashedSlots<S,T> makeSlots() {
    return new HashedSlots<S,T>();
  }
  
  public <T> SlotInfo<T>  newAttribute() {
    return makeSlotInfo(this.<T>getStorage());
  }
  
  public <T> SlotInfo<T>  newLabeledAttribute(String label) {
	  return makeLabeledSlotInfo(this.<T>getStorage(), label);
  }
  
  public <T> SlotInfo<T>  newLabeledAttribute(String label, T defaultValue) {
	  return makeLabeledSlotInfo(this.<T>getStorage(), label, defaultValue);
  }

  public <T> SlotInfo<T>  newAttribute(T defaultValue) {
    return makeSlotInfo(this.<T>getStorage(),defaultValue);
  }

  public <T> SlotInfo<T>  newAttribute(String name, IRType<T> type) throws SlotAlreadyRegisteredException {
    return makeSlotInfo(this.<T>getStorage(),name,type);
  }

  public <T> SlotInfo<T>  newAttribute(String name, IRType<T> type, T defaultValue) throws SlotAlreadyRegisteredException {
    return makeSlotInfo(this.<T>getStorage(),name,type,defaultValue);
  }

  public static <S,T> SlotInfo<T> makeSlotInfo(SlotStorage<S,T> storage) {
    return new InfoStoredSlotInfo<S,T>(storage,null,AbstractSlotFactory.<S,T>makeSlots());
  }
  public static <S,T> SlotInfo<T> makeLabeledSlotInfo(SlotStorage<S,T> storage, String label) {
	    return new InfoStoredSlotInfo<S,T>(storage, label, AbstractSlotFactory.<S,T>makeSlots());
  }
  public static <S,T> SlotInfo<T> makeLabeledSlotInfo(SlotStorage<S,T> storage, String label, T defaultValue) {
	    return new InfoStoredSlotInfo<S,T>(storage, label, defaultValue, AbstractSlotFactory.<S,T>makeSlots());
  }
  public static <S,T> SlotInfo<T> makeSlotInfo(SlotStorage<S,T> storage, String name, IRType<T> type) 
       throws SlotAlreadyRegisteredException
  {
    return new InfoStoredSlotInfo<S,T>(name,type,storage,AbstractSlotFactory.<S,T>makeSlots());
  }
  public static <S,T> SlotInfo<T> makeSlotInfo(SlotStorage<S,T> storage, T defaultValue) {
    return new InfoStoredSlotInfo<S,T>(storage,null,defaultValue,AbstractSlotFactory.<S,T>makeSlots());
  }
  public static <S,T> SlotInfo<T> makeSlotInfo(SlotStorage<S,T> storage, String name, IRType<T> type, T defaultValue)
       throws SlotAlreadyRegisteredException
  {
    return new InfoStoredSlotInfo<S,T>(name,type,storage,defaultValue,AbstractSlotFactory.<S,T>makeSlots());
  }

  
  public AbstractChangeRecord newChangeRecord(String name) throws SlotAlreadyRegisteredException {
    return new SimpleChangeRecord(name);
  }

  public <T> Iteratable<T> newIterator(Iteratable<T> e) {
    return e;
  }
  
  public <T> ListIteratable<T> newListIterator(ListIteratable<T> e) {
    return e;
  }

  /**
   * Create a new list/array or 1ARray depending on the initial size.
   * This method should be overridden for the 1ARray case because
   * then one field can be saved in these (common) nodes.
   * @see edu.cmu.cs.fluid.ir.SlotFactory#newSequence(int)
   */
  public <T> IRSequence<T> newSequence(int size) {
    if (size < 0) {
      return makeGenericIRList(~size,this.<Integer>getStorage(),
          this.<T>getStorage(),
          this.<IRLocation>getStorage());
    } else if (size == 0) {
      return EmptyIRSequence.prototype();
    } else if (size == 1) {
      return makeGenericIR1Array(this.<T>getStorage());
    } else if (size == 2) {
      return makeGenericIR2Array(this.<T>getStorage());
    } else {
      return makeIRArray(size,this.<T>getStorage());
    }
  }

  private <S,T> IR1Array<S,T> makeGenericIR1Array(final SlotStorage<S,T> storage) {
    return new IR1Array<S,T>() {
      @Override
      public SlotStorage<S,T> getSlotStorage() { return storage; }
    };
  }
  private <S,T> IR2Array<S,T> makeGenericIR2Array(final SlotStorage<S,T> storage) {
    return new  IR2Array<S,T>() {
      @Override
      public SlotStorage<S,T> getSlotStorage() { return storage; }
    };
  }

  private <S,T> IRArray<S,T> makeIRArray(int n, SlotStorage<S,T> storage) {
    return new IRArray<S,T>(n, storage);
  }
  
  private <IntS,S,ES,T> IRList<IntS,S,ES,T> 
  makeGenericIRList(int ss, 
      final SlotStorage<IntS,Integer> ints,
      final SlotStorage<S,T> storage,
      final SlotStorage<ES,IRLocation> ls) {
    return new  IRList<IntS,S,ES,T>(ss) {
      @Override
      public SlotFactory getSlotFactory() { return storage.getSlotFactory(); }
      @Override
      public SlotStorage<IntS,Integer> getIntSlotStorage() { return ints; }
      @Override
      public SlotStorage<S,T> getSlotStorage() { return storage; }
      @Override
      public SlotStorage<ES,IRLocation> getElemStorage() { return ls; }
    };
  }
}
