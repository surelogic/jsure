/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/AbstractImplicitSlotFactory.java,v 1.5 2007/07/10 22:16:31 aarong Exp $
 */
package edu.cmu.cs.fluid.ir;


/**
 * A slot factory which doesn't use explicit slots.
 * Neither does it record change information at this granularity, the slot state
 * is simply the value of the slot.
 * @author boyland
 */
public abstract class AbstractImplicitSlotFactory extends AbstractSlotFactory {
  private final SlotStorage<Object,Object> storage = new ImplicitSlotStorage<Object>(this);

  @SuppressWarnings("unchecked") // the cast doesn't cause a problem.
  public <T> SlotStorage<T,T> getStorage() {
    return (SlotStorage<T,T>)storage; // or // new ImplicitSlotStorage<T>(); // wastes memory
  }
}
