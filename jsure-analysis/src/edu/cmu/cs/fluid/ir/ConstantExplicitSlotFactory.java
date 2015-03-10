/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/ConstantExplicitSlotFactory.java,v 1.3 2006/03/27 21:35:50 boyland Exp $
 */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;


/**
 * A slot factory that uses explicit constant slots.
 * Normally one need not use them, butrather use implicit slots
 * @see ConstantSlotFactory
 * @author boyland
 */
public class ConstantExplicitSlotFactory extends AbstractExplicitSlotFactory {
  protected ConstantExplicitSlotFactory() {}
  public static final ConstantExplicitSlotFactory prototype = 
       new ConstantExplicitSlotFactory();
  static {
    IRPersistent.registerSlotFactory(prototype,'c');
  }

  @Override
  public <T> Slot<T> undefinedSlot() {
    return new UndefinedConstantSlot<T>();
  }
  @Override
  public <T> Slot<T> predefinedSlot(T value) {
    return new ConstantSlot<T>(value);
  }
  
  
  @Override
  public void noteChange(IRState state) {
    // Do nothing (no changes for constant slots);
  }
  @Override
  public SlotFactory getOldFactory() {
    return OldConstantExplicitSlotFactory.prototype;
  }
  
  
}

class OldConstantExplicitSlotFactory extends ConstantExplicitSlotFactory {
  public static SlotFactory prototype = new OldConstantExplicitSlotFactory();
  @SuppressWarnings("unchecked")
  @Override
  public <T> Slot<T> undefinedSlot() {
    return UndefinableConstantSlot.prototype;
  }
}

/** A constant slot created in another JVM: can only be set using
 * persistence, never by "setSlotValue".
 */
class UndefinableConstantSlot<T> extends UndefinedSlot<T> {
  public static final UndefinedSlot prototype = new UndefinableConstantSlot();

  @Override
  public T getValue() throws SlotUnknownException {
    throw new SlotUnknownException("Slot value not loaded.",this);
  }

  @Override
  public Slot<T> setValue(T newValue) throws SlotImmutableException {
    throw new SlotImmutableException("Slot cannot be defined");
  }

  @Override
  public Slot<T> readValue(IRType<T> ty, IRInput in) 
     throws IOException
  {
    return new ConstantSlot<T>(ty.readValue(in));
  }
}
